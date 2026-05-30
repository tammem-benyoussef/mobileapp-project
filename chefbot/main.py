from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import List, Optional
import os
import json

from dotenv import load_dotenv
from groq import Groq

load_dotenv()

app = FastAPI(title="Chef AI API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

GROQ_API_KEY = os.getenv("GROQ_API_KEY")


def get_groq_client() -> Groq:
    if not GROQ_API_KEY:
        raise HTTPException(status_code=500, detail="GROQ_API_KEY is not set.")
    try:
        return Groq(api_key=GROQ_API_KEY)
    except Exception as exc:
        raise HTTPException(status_code=500, detail=f"Failed to initialize Groq client: {exc}")


CHEF_SYSTEM_PROMPT = """You are Chef Marco, a world-renowned professional chef with over 25 years of culinary experience across Michelin-starred restaurants in Paris, Tokyo, and New York. You trained under legendary chefs and have mastered cuisines from around the world — French, Italian, Japanese, Middle Eastern, and beyond.

Your personality:
- Warm, passionate, and deeply knowledgeable about food
- You speak with the confidence and precision of a seasoned professional
- You use culinary terminology naturally but always explain it when needed
- You're enthusiastic about sharing your craft and inspiring home cooks
- You occasionally sprinkle in personal anecdotes from your career in kitchens

Your expertise covers:
- Classic and contemporary recipes with precise measurements and techniques
- Flavor pairing and ingredient substitutions
- Cooking techniques: braising, sautéing, sous vide, emulsification, tempering, etc.
- Dietary adaptations (vegan, gluten-free, dairy-free, etc.)
- Kitchen tips, equipment recommendations, and food safety
- Wine and beverage pairings
- Plating and presentation advice

When giving recipes:
1. Always start with a brief, enticing description of the dish
2. List ingredients with precise measurements (metric and imperial)
3. Provide clear, step-by-step instructions
4. Include timing, temperatures, and visual cues for doneness
5. Add Chef's Tips for common mistakes to avoid
6. Suggest variations or serving recommendations

Always stay in character as Chef Marco. Never break character or discuss topics unrelated to food, cooking, recipes, and culinary arts. If asked about non-food topics, gracefully redirect the conversation back to cuisine."""


class Message(BaseModel):
    role: str
    content: str


class ChatRequest(BaseModel):
    messages: List[Message]
    stream: Optional[bool] = False
    model: Optional[str] = "llama-3.3-70b-versatile"


class ChatResponse(BaseModel):
    reply: str
    model: str
    usage: dict


@app.get("/")
def root():
    return {"service": "Chef AI API", "status": "running", "chef": "Chef Marco", "docs": "/docs"}


@app.get("/health")
def health():
    return {"status": "healthy"}


@app.get("/models")
def list_models():
    return {
        "recommended": "llama-3.3-70b-versatile",
        "available": [
            {"id": "llama-3.3-70b-versatile", "name": "LLaMA 3.3 70B (Recommended)"},
            {"id": "llama-3.1-8b-instant", "name": "LLaMA 3.1 8B (Faster)"},
            {"id": "mixtral-8x7b-32768", "name": "Mixtral 8x7B"},
            {"id": "gemma2-9b-it", "name": "Gemma 2 9B"},
        ],
    }


@app.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest):
    client = get_groq_client()
    groq_messages = [{"role": "system", "content": CHEF_SYSTEM_PROMPT}]
    for msg in request.messages:
        if msg.role not in ("user", "assistant"):
            raise HTTPException(status_code=400, detail=f"Invalid role: {msg.role}")
        groq_messages.append({"role": msg.role, "content": msg.content})

    try:
        completion = client.chat.completions.create(
            model=request.model,
            messages=groq_messages,
            temperature=0.7,
            max_tokens=2048,
        )
        reply = completion.choices[0].message.content
        usage = {
            "prompt_tokens": completion.usage.prompt_tokens,
            "completion_tokens": completion.usage.completion_tokens,
            "total_tokens": completion.usage.total_tokens,
        }
        return ChatResponse(reply=reply, model=completion.model, usage=usage)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Groq API error: {str(e)}")


@app.post("/chat/stream")
def chat_stream(request: ChatRequest):
    client = get_groq_client()
    groq_messages = [{"role": "system", "content": CHEF_SYSTEM_PROMPT}]
    for msg in request.messages:
        groq_messages.append({"role": msg.role, "content": msg.content})

    def generate():
        try:
            stream = client.chat.completions.create(
                model=request.model,
                messages=groq_messages,
                temperature=0.7,
                max_tokens=2048,
                stream=True,
            )
            for chunk in stream:
                delta = chunk.choices[0].delta
                if delta and delta.content:
                    data = json.dumps({"token": delta.content})
                    yield f"data: {data}\n\n"
            yield "data: [DONE]\n\n"
        except Exception as e:
            yield f"data: {json.dumps({'error': str(e)})}\n\n"

    return StreamingResponse(generate(), media_type="text/event-stream")


@app.post("/recipe/suggest")
def suggest_recipe(ingredients: List[str], dietary: Optional[str] = None):
    ingredient_list = ", ".join(ingredients)
    prompt = f"I have these ingredients: {ingredient_list}."
    if dietary:
        prompt += f" I follow a {dietary} diet."
    prompt += " What's the best dish you can suggest with what I have?"
    request = ChatRequest(messages=[Message(role="user", content=prompt)])
    return chat(request)