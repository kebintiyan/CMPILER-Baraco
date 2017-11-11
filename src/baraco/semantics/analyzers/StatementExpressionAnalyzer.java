package baraco.semantics.analyzers;

import baraco.antlr.lexer.BaracoLexer;
import baraco.antlr.parser.BaracoParser.*;
import baraco.execution.ExecutionManager;
import baraco.execution.commands.ICommand;
import baraco.execution.commands.controlled.IConditionalCommand;
import baraco.execution.commands.controlled.IControlledCommand;
import baraco.execution.commands.evaluation.AssignmentCommand;
import baraco.execution.commands.simple.IncDecCommand;
import baraco.execution.commands.simple.MethodCallCommand;
import baraco.semantics.statements.StatementControlOverseer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

public class StatementExpressionAnalyzer implements ParseTreeListener {

    private ExpressionContext readRightHandExprCtx; //used to avoid mistakenly reading right hand expressions as direct function calls as well.

    //TODO: find a way to not rely on tree depth for function calls.
    public final static int FUNCTION_CALL_NO_PARAMS_DEPTH = 13;
    public final static int FUNCTION_CALL_WITH_PARAMS_DEPTH = 14;

    public StatementExpressionAnalyzer() {

    }

    public void analyze(StatementExpressionContext statementExprCtx) {
        ParseTreeWalker treeWalker = new ParseTreeWalker();
        treeWalker.walk(this, statementExprCtx);
    }

    public void analyze(ExpressionContext exprCtx) {
        ParseTreeWalker treeWalker = new ParseTreeWalker();
        treeWalker.walk(this, exprCtx);
    }

    @Override
    public void visitTerminal(TerminalNode node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        // TODO Auto-generated method stub

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        if(ctx instanceof ExpressionContext) {
            ExpressionContext exprCtx = (ExpressionContext) ctx;

            if(isAssignmentExpression(exprCtx)) {
                System.out.println("Assignment expr detected: " +exprCtx.getText());

                List<ExpressionContext> exprListCtx = exprCtx.expression();
                AssignmentCommand assignmentCommand = new AssignmentCommand(exprListCtx.get(0), exprListCtx.get(1));

                this.readRightHandExprCtx = exprListCtx.get(1);
                this.handleStatementExecution(assignmentCommand);

            }
            else if(isIncrementExpression(exprCtx)) {
                System.out.println("Increment expr detected: " +exprCtx.getText());

                List<ExpressionContext> exprListCtx = exprCtx.expression();

                IncDecCommand incDecCommand = new IncDecCommand(exprListCtx.get(0), BaracoLexer.INC);
                this.handleStatementExecution(incDecCommand);
            }

            else if(isDecrementExpression(exprCtx)) {
                System.out.println("Decrement expr detected: " +exprCtx.getText());

                List<ExpressionContext> exprListCtx = exprCtx.expression();

                IncDecCommand incDecCommand = new IncDecCommand(exprListCtx.get(0), BaracoLexer.DEC);
                this.handleStatementExecution(incDecCommand);

            }

            else if(this.isFunctionCallWithParams(exprCtx)) {
                this.handleFunctionCallWithParams(exprCtx);
            }

            else if(this.isFunctionCallWithNoParams(exprCtx)) {
                this.handleFunctionCallWithNoParams(exprCtx);
            }
        }
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        // TODO Auto-generated method stub

    }

    private void handleStatementExecution(ICommand command) {

        StatementControlOverseer statementControl = StatementControlOverseer.getInstance();

        //add to conditional controlled command
        if(statementControl.isInConditionalCommand()) {
            IConditionalCommand conditionalCommand = (IConditionalCommand) statementControl.getActiveControlledCommand();

            if(statementControl.isInPositiveRule()) {
                conditionalCommand.addPositiveCommand(command);
            }
            else {
                conditionalCommand.addNegativeCommand(command);
            }
        }

        else if(statementControl.isInControlledCommand()) {
            IControlledCommand controlledCommand = (IControlledCommand) statementControl.getActiveControlledCommand();
            controlledCommand.addCommand(command);
        }
        else {
            ExecutionManager.getInstance().addCommand(command);
        }

    }

    private void handleFunctionCallWithParams(ExpressionContext funcExprCtx) {
        ExpressionContext functionExprCtx = funcExprCtx.expression(0);
        String functionName = functionExprCtx.Identifier().getText();

        MethodCallCommand functionCallCommand = new MethodCallCommand(functionName, funcExprCtx);
        this.handleStatementExecution(functionCallCommand);

        System.out.println("Function call with params detected: " +functionName);
    }

    private void handleFunctionCallWithNoParams(ExpressionContext funcExprCtx) {
        String functionName = funcExprCtx.Identifier().getText();

        MethodCallCommand methodCallCommand = new MethodCallCommand(functionName, funcExprCtx);
        this.handleStatementExecution(methodCallCommand);

        System.out.println("Function call with no params detected: " +functionName);
    }

    public static boolean isAssignmentExpression(ExpressionContext exprCtx) {
        List<TerminalNode> tokenList = exprCtx.getTokens(BaracoLexer.ASSIGN);
        return (tokenList.size() > 0);
    }

    public static boolean isIncrementExpression(ExpressionContext exprCtx) {
        List<TerminalNode> incrementList = exprCtx.getTokens(BaracoLexer.INC);

        return (incrementList.size() > 0);
    }

    public static boolean isDecrementExpression(ExpressionContext exprCtx) {
        List<TerminalNode> decrementList = exprCtx.getTokens(BaracoLexer.DEC);

        return (decrementList.size() > 0);
    }

    private boolean isFunctionCallWithParams(ExpressionContext exprCtx) {
        ExpressionContext firstExprCtx = exprCtx.expression(0);

        if(firstExprCtx != null) {
            if(exprCtx != this.readRightHandExprCtx) {
                //ThisKeywordChecker thisChecker = new ThisKeywordChecker(firstExprCtx);
                //thisChecker.verify();

                return (firstExprCtx.Identifier() != null);
            }
        }

        return false;

    }

    private boolean isFunctionCallWithNoParams(ExpressionContext exprCtx) {
        if(exprCtx.depth() == FUNCTION_CALL_NO_PARAMS_DEPTH) {
            //ThisKeywordChecker thisChecker = new ThisKeywordChecker(exprCtx);
            //thisChecker.verify();
            if(exprCtx.Identifier() != null)
                return true;
        }

        return false;
    }
}