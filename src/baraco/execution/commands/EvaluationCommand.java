package baraco.execution.commands;

import baraco.antlr.parser.BaracoParser.*;
import baraco.builder.ParserHandler;
import baraco.representations.*;
import baraco.semantics.searching.VariableSearcher;
import baraco.semantics.symboltable.SymbolTableManager;
import baraco.semantics.symboltable.scopes.ClassScope;
import baraco.semantics.utils.Expression;
import baraco.semantics.utils.StringUtils;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

public class EvaluationCommand implements ICommand, ParseTreeListener {

    private final static String TAG = "EvaluationCommand";

    private ExpressionContext parentExprCtx;
    private String modifiedExp;
    private BigDecimal resultValue;
    private String stringResult = "";

    private String prevFuncEvaluated;

    private boolean isNumeric;

    public EvaluationCommand(ExpressionContext exprCtx) {
        this.parentExprCtx = exprCtx;
    }

    @Override
    public void execute() {

        System.out.println("EvaluationCommand: executing");
        this.modifiedExp = this.parentExprCtx.getText();

        for (ExpressionContext eCtx : this.parentExprCtx.expression()) { // bias functions in evaluating
            if (isFunctionCall(eCtx)) {
                EvaluationCommand evaluationCommand = new EvaluationCommand(eCtx);
                evaluationCommand.execute();

                this.modifiedExp = this.modifiedExp.replace(eCtx.getText(), evaluationCommand.modifiedExp);
            }
        }

        ParseTreeWalker treeWalker = new ParseTreeWalker();
        treeWalker.walk(this, this.parentExprCtx);

        isNumeric = !this.modifiedExp.contains("\"");

        if (this.modifiedExp.contains(RecognizedKeywords.BOOLEAN_TRUE)) {

            this.resultValue = new BigDecimal(1);
            this.stringResult = this.resultValue.toEngineeringString();

        } else if (this.modifiedExp.contains(RecognizedKeywords.BOOLEAN_FALSE)) {

            this.resultValue = new BigDecimal(0);
            this.stringResult = this.resultValue.toEngineeringString();

        } else if (!isNumeric) {

            if (this.parentExprCtx.expression().size() != 0 &&
                    !isArrayElement(parentExprCtx) &&
                    !isFunctionCall(parentExprCtx)) {

                for (ExpressionContext expCtx :
                        this.parentExprCtx.expression()) {

                    System.out.println("start this " + this.parentExprCtx.getText());

                    EvaluationCommand innerEvCmd = new EvaluationCommand(expCtx);
                    innerEvCmd.execute();

                    if (isNumericResult())
                        this.stringResult += innerEvCmd.getResult();
                    else
                        this.stringResult += innerEvCmd.getStringResult();
                }

            } else {
                this.stringResult = StringUtils.removeQuotes(modifiedExp);
            }

        } else {
            Expression evalEx = new Expression(this.modifiedExp);

            System.out.println(this.modifiedExp);

            //Log.i(TAG,"Modified exp to eval: " +this.modifiedExp);
            this.resultValue = evalEx.eval();
            this.stringResult = this.resultValue.toEngineeringString();

        }

    }

    @Override
    public void visitTerminal(TerminalNode node) {

    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if (ctx instanceof ExpressionContext) {
            ExpressionContext exprCtx = (ExpressionContext) ctx;

            if (EvaluationCommand.isFunctionCall(exprCtx)) {
                this.evaluateFunctionCall(exprCtx);
            } else if (EvaluationCommand.isArrayElement(exprCtx)) {
                this.evaluateArray(exprCtx);
            } else if (EvaluationCommand.isVariableOrConst(exprCtx)) {
                this.evaluateVariable(exprCtx);
            }
        }
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {

    }

    public static boolean isFunctionCall(ExpressionContext exprCtx) {
        Pattern functionPattern = Pattern.compile("([a-zA-Z0-9]+)\\(([ ,.a-zA-Z0-9]*)\\)");

        if (exprCtx.expressionList() != null || functionPattern.matcher(exprCtx.getText()).matches()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isVariableOrConst(ExpressionContext exprCtx) {
        if (exprCtx.primary() != null && exprCtx.primary().Identifier() != null) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isArrayElement(ExpressionContext exprCtx) {
        if (exprCtx.expression(0) != null && exprCtx.expression(1) != null) {
            BaracoValue value = BaracoValueSearcher.searchBaracoValue(exprCtx.expression(0).getText());

            if (value != null)
                return value.getPrimitiveType() == BaracoValue.PrimitiveType.ARRAY;
            else
                return false;
        } else {
            return false;
        }
    }

    private void evaluateFunctionCall(ExpressionContext exprCtx) {

        for (ExpressionContext eCtx : exprCtx.expression()) {

            String functionName = eCtx.getText();

            ClassScope classScope = SymbolTableManager.getInstance().getClassScope(
                    ParserHandler.getInstance().getCurrentClassName());
            BaracoMethod baracoMethod = classScope.searchMethod(functionName);

        /*if (exprCtx.arguments().expressionList() != null) {
            List<ExpressionContext> exprCtxList = exprCtx.arguments()
                    .expressionList().expression();

            for (int i = 0; i < exprCtxList.size(); i++) {
                ExpressionContext parameterExprCtx = exprCtxList.get(i);

                EvaluationCommand evaluationCommand = new EvaluationCommand(parameterExprCtx);
                evaluationCommand.execute();

                baracoMethod.mapParameterByValueAt(evaluationCommand.getResult().toEngineeringString(), i);
            }
        }*/

            if (baracoMethod == null) {
                return;
            }

            List<ExpressionContext> exprCtxList;

            if (eCtx.expressionList() != null) {
                exprCtxList = eCtx.expressionList().expression();
            } else {
                exprCtxList = exprCtx.expressionList().expression();
            }

            for (int i = 0; i < exprCtxList.size(); i++) {
                ExpressionContext parameterExprCtx = exprCtxList.get(i);

                EvaluationCommand evaluationCommand = new EvaluationCommand(parameterExprCtx);
                evaluationCommand.execute();

                if (evaluationCommand.isNumericResult())
                    baracoMethod.mapParameterByValueAt(evaluationCommand.getResult().toEngineeringString(), i);
                else
                    baracoMethod.mapParameterByValueAt(evaluationCommand.getStringResult(), i);
            }

            baracoMethod.execute();

            System.out.println(TAG + ": " + "Before modified EXP function call: " + this.modifiedExp);
            this.modifiedExp = this.modifiedExp.replace(exprCtx.getText(),
                    baracoMethod.getReturnValue().getValue().toString());

            if (baracoMethod.getReturnType() == BaracoMethod.MethodType.STRING_TYPE)
                this.modifiedExp = "\"" + this.modifiedExp + "\"";

            System.out.println(TAG + ": " + "After modified EXP function call: " + this.modifiedExp);

        }
    }

    private void evaluateVariable(ExpressionContext exprCtx) {
        BaracoValue baracoValue = VariableSearcher
                .searchVariable(exprCtx.getText());

        if (baracoValue == null || baracoValue.getPrimitiveType() == BaracoValue.PrimitiveType.ARRAY) {
            return;
        }

        this.modifiedExp = this.modifiedExp.replaceFirst(exprCtx.getText(),
                baracoValue.getValue().toString());

        if (baracoValue.getPrimitiveType() == BaracoValue.PrimitiveType.STRING)
            modifiedExp = "\"" + modifiedExp + "\"";

        //System.out.println("EVALUATED: " + modifiedExp);
    }

    private void evaluateArray(ExpressionContext exprCtx) {
        BaracoValue value = BaracoValueSearcher.searchBaracoValue(exprCtx.expression(0).getText());

        if (value != null) {
            if (value.getPrimitiveType() == BaracoValue.PrimitiveType.ARRAY) {

                BaracoArray baracoArray = (BaracoArray) value.getValue();

                EvaluationCommand evCmd = new EvaluationCommand(exprCtx.expression(1));
                evCmd.execute();

                System.out.println("The result : " + evCmd.getResult().intValue());

                BaracoValue arrayMobiValue = baracoArray.getValueAt(evCmd.getResult().intValue());

                System.out.println("The value : " + arrayMobiValue.getValue().toString());
                System.out.println(this.modifiedExp.replace(exprCtx.getText(), "\"" + arrayMobiValue.getValue().toString() + "\""));

                if (arrayMobiValue.getPrimitiveType() == BaracoValue.PrimitiveType.STRING) {
                    //this.modifiedExp = this.modifiedExp.replaceFirst(exprCtx.expression(0).getText() + "\\[([a-zA-Z0-9]*)]", "\"" + arrayMobiValue.getValue().toString() + "\"");
                    this.modifiedExp = this.modifiedExp.replace(exprCtx.getText(), "\"" + arrayMobiValue.getValue().toString() + "\"");
                } else {
                    //this.modifiedExp = this.modifiedExp.replaceFirst(exprCtx.expression(0).getText() + "\\[([a-zA-Z0-9]*)]", arrayMobiValue.getValue().toString());
                    this.modifiedExp = this.modifiedExp.replace(exprCtx.getText(), arrayMobiValue.getValue().toString());
                }

                System.out.println("@ " + this.parentExprCtx.getText() +" EVALUATED ARRAY " + exprCtx.getText() + ":" + modifiedExp);
            }
        }

    }

    /*
     * Returns the result
     */
    public BigDecimal getResult() {
        return this.resultValue;
    }

    public String getStringResult() {
        return stringResult;
    }

    public boolean isNumericResult() {
        return isNumeric;
    }
}
