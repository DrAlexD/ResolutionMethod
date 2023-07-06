import java.util.ArrayList;

public class Parser {
    static ArrayList<Token> tokens = new ArrayList<>();

    private int numberOfCurrentToken;
    private Token currentToken;
    private LogicExpression taskExpression;

    private void nextTok() {
        currentToken = tokens.get(numberOfCurrentToken);
        numberOfCurrentToken++;
    }

    public void parse() throws CloneNotSupportedException {
        numberOfCurrentToken = 1;
        currentToken = tokens.get(0);
        parseProg();
        ResolutionMethod.logicExpressions.add(new LogicExpression(DomainTag.INVERSION, taskExpression));
    }

    //Prog = {Rule} Task
    private void parseProg() throws CloneNotSupportedException {
        if (currentToken.tag == DomainTag.DASH) {
            while (currentToken.tag == DomainTag.DASH) {
                parseRule();
            }

            if (currentToken.tag == DomainTag.QUESTION_SIGN) {
                parseTask();
                if (currentToken.tag != DomainTag.END_OF_PROGRAM)
                    endProgram("expected end_of_program");
            } else
                endProgram("expected question_sign");
        } else
            endProgram("expected dash");
    }

    //Rule = '-' LogicExprWithInversion {Operation LogicExprWithInversion}
    private void parseRule() throws CloneNotSupportedException {
        if (currentToken.tag == DomainTag.DASH) {
            nextTok();

            ArrayList<DomainTag> operations = new ArrayList<>();
            ArrayList<LogicExpression> expressions = new ArrayList<>();

            LogicExpression expr = parseLogicExprWithInversion();
            expressions.add(expr);

            while (currentToken.tag == DomainTag.CONJUNCTION || currentToken.tag == DomainTag.DISJUNCTION ||
                    currentToken.tag == DomainTag.IMPLICATION || currentToken.tag == DomainTag.IDENTITY) {
                DomainTag operation = currentToken.tag;
                operations.add(operation);

                nextTok();

                LogicExpression expr2 = parseLogicExprWithInversion();
                expressions.add(expr2);
            }

            expr = chooseOperationByPriorityForPlaceBrackets(expressions, operations);
            ResolutionMethod.logicExpressions.add(expr);
        } else
            endProgram("expected dash");
    }

    //LogicExprWithInversion = '~' LogicExpr | LogicExpr
    private LogicExpression parseLogicExprWithInversion() throws CloneNotSupportedException {
        LogicExpression expr;
        LogicExpression returnedExpr = null;

        DomainTag inversionTag = null;
        if (currentToken.tag == DomainTag.INVERSION) {
            inversionTag = currentToken.tag;
            nextTok();
        }

        if (currentToken.tag == DomainTag.VARIABLE || currentToken.tag == DomainTag.LEFT_BRACKET) {
            returnedExpr = parseLogicExpr();
        } else
            endProgram("expected var or left_bracket");

        if (inversionTag != null)
            expr = new LogicExpression(inversionTag, returnedExpr);
        else
            expr = returnedExpr;

        return expr;
    }

    //LogicExpr = var | LogicExprInBrackets
    private LogicExpression parseLogicExpr() throws CloneNotSupportedException {
        LogicExpression expr = null;

        if (currentToken.tag == DomainTag.LEFT_BRACKET) {
            expr = parseLogicExprInBrackets();
        } else if (currentToken.tag == DomainTag.VARIABLE) {
            expr = new LogicExpression(currentToken.clone());
            nextTok();
        } else
            endProgram("expected left_bracket or variable");

        return expr;
    }

    //LogicExprInBrackets = '(' LogicExprWithInversion {LogicExprWithInversion} ')'
    private LogicExpression parseLogicExprInBrackets() throws CloneNotSupportedException {
        LogicExpression expr = null;

        if (currentToken.tag == DomainTag.LEFT_BRACKET) {
            nextTok();

            ArrayList<DomainTag> operations = new ArrayList<>();
            ArrayList<LogicExpression> expressions = new ArrayList<>();

            expr = parseLogicExprWithInversion();
            expressions.add(expr);

            while (currentToken.tag == DomainTag.CONJUNCTION || currentToken.tag == DomainTag.DISJUNCTION ||
                    currentToken.tag == DomainTag.IMPLICATION || currentToken.tag == DomainTag.IDENTITY) {
                DomainTag operation = currentToken.tag;
                operations.add(operation);

                nextTok();

                LogicExpression expr2 = parseLogicExprWithInversion();
                expressions.add(expr2);
            }

            expr = chooseOperationByPriorityForPlaceBrackets(expressions, operations);

            if (currentToken.tag == DomainTag.RIGHT_BRACKET) {
                nextTok();
            } else
                endProgram("expected right_bracket");
        } else
            endProgram("expected left_bracket");

        return expr;
    }

    //Task = '?' LogicExprWithInversion {Operation LogicExprWithInversion}
    private void parseTask() throws CloneNotSupportedException {
        if (currentToken.tag == DomainTag.QUESTION_SIGN) {
            nextTok();

            ArrayList<DomainTag> operations = new ArrayList<>();
            ArrayList<LogicExpression> expressions = new ArrayList<>();

            LogicExpression expr = parseLogicExprWithInversion();
            expressions.add(expr);

            while (currentToken.tag == DomainTag.CONJUNCTION || currentToken.tag == DomainTag.DISJUNCTION ||
                    currentToken.tag == DomainTag.IMPLICATION || currentToken.tag == DomainTag.IDENTITY) {
                DomainTag operation = currentToken.tag;
                operations.add(operation);

                nextTok();

                LogicExpression expr2 = parseLogicExprWithInversion();
                expressions.add(expr2);
            }

            taskExpression = chooseOperationByPriorityForPlaceBrackets(expressions, operations);
        } else
            endProgram("expected question_sign");
    }

    private LogicExpression chooseOperationByPriorityForPlaceBrackets
            (ArrayList<LogicExpression> expressions, ArrayList<DomainTag> operations) {

        if (expressions.size() == 1)
            return expressions.get(0);

        LogicExpression expr;
        expr = placeBracketsForChosenOperation(expressions, operations, DomainTag.CONJUNCTION);
        if (expr == null)
            expr = placeBracketsForChosenOperation(expressions, operations, DomainTag.DISJUNCTION);
        if (expr == null)
            expr = placeBracketsForChosenOperation(expressions, operations, DomainTag.IMPLICATION);
        if (expr == null)
            expr = placeBracketsForChosenOperation(expressions, operations, DomainTag.IDENTITY);

        return expr;
    }

    private LogicExpression placeBracketsForChosenOperation
            (ArrayList<LogicExpression> expressions, ArrayList<DomainTag> operations, DomainTag tag) {

        for (int i = 0; i < operations.size(); i++) {
            if (operations.get(i) == tag) {
                LogicExpression expr = expressions.remove(i + 1);
                DomainTag operation = operations.remove(i);
                expressions.set(i, new LogicExpression(expressions.get(i), operation, expr));
                return chooseOperationByPriorityForPlaceBrackets(expressions, operations);
            }
        }

        return null;
    }

    private void endProgram(String mes) {
        System.out.println("ERROR" + currentToken.coords + ": " + mes);
        System.exit(1);
    }
}
