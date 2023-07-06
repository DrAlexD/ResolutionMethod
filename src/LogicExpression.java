class LogicExpression {
    private LogicExpression firstExpr = null;
    private DomainTag operation = null;
    private LogicExpression secondExpr = null;

    private Token variable = null;

    public LogicExpression(LogicExpression firstExpr, DomainTag operation, LogicExpression secondExpr) {
        this.firstExpr = firstExpr;
        this.operation = operation;
        this.secondExpr = secondExpr;
    }

    public LogicExpression(DomainTag operation, LogicExpression secondExpr) {
        this.operation = operation;
        this.secondExpr = secondExpr;
    }

    public LogicExpression(Token variable) {
        this.variable = variable;
    }

    public LogicExpression getFirstExpr() {
        return firstExpr;
    }

    public void setFirstExpr(LogicExpression firstExpr) {
        this.firstExpr = firstExpr;
    }

    public DomainTag getOperation() {
        return operation;
    }

    public void setOperation(DomainTag operation) {
        this.operation = operation;
    }

    public LogicExpression getSecondExpr() {
        return secondExpr;
    }

    public void setSecondExpr(LogicExpression secondExpr) {
        this.secondExpr = secondExpr;
    }

    public Token getVariable() {
        return variable;
    }

    public void setVariable(Token variable) {
        this.variable = variable;
    }

    public String printExpr(boolean isBracketsAroundCurrentExpr) {
        String text = "";
        if (variable != null)
            text = variable.attr;
        else if (firstExpr != null) {
            if (isBracketsAroundCurrentExpr)
                text = "(";

            text += firstExpr.printExpr(operation != firstExpr.operation) + " " +
                    operation.text + " " +
                    secondExpr.printExpr(operation != secondExpr.operation);

            if (isBracketsAroundCurrentExpr)
                text += ")";
        } else
            text = operation.text + secondExpr.printExpr(operation != secondExpr.operation);

        return text;
    }
}