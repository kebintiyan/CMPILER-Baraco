package baraco.templates;

import baraco.representations.BaracoValue;

import java.util.ArrayList;

public class BaracoMethodTemplate {

    String methodName;
    String returnType;
    boolean isPublic;
    ArrayList<BaracoMethodTemplateParameter> parameters;

    public BaracoMethodTemplate() {
        this.parameters = new ArrayList<>();
    }

    private String getMethodName() {
        return this.methodName;
    }

    public BaracoMethodTemplate setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public BaracoMethodTemplate setReturnType(String returnType) {
        this.returnType = returnType;
        return this;
    }

    public BaracoMethodTemplate setIsPublic(boolean isPublic) {
        this.isPublic = isPublic;
        return this;
    }

    public BaracoMethodTemplate addParameter(BaracoMethodTemplateParameter parameter) {
        this.parameters.add(parameter);
        return this;
    }

    public boolean hasParameter(BaracoMethodTemplateParameter parameter) {
        for (BaracoMethodTemplateParameter param : parameters) {
            if (param.getParameterName().equals(parameter.getParameterName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        String method = "\t" + (isPublic ? "public " : "private ") + returnType + " " + methodName + "(";

        for (int i = 0; i < parameters.size() - 1; i++) {
            method += parameters.get(i).toString() + ", ";
        }

        if (parameters.size() > 0) {
            method += parameters.get(parameters.size() - 1).toString();
        }

        method += "):\n\t\t" +
                "// Code goes here";

        method += "\n\tend";


        return method;
    }
}


