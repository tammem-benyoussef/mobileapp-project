package com.example.hamhama.data.remote.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyzedInstructionDto {

    private String name;
    private List<InstructionStepDto> steps = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<InstructionStepDto> getSteps() {
        return steps;
    }

    public void setSteps(List<InstructionStepDto> steps) {
        this.steps = steps;
    }
}