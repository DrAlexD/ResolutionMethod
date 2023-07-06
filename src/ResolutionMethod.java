import java.util.ArrayList;
import java.util.List;

public class ResolutionMethod {
    static ArrayList<LogicExpression> logicExpressions = new ArrayList<>();

    private final ArrayList<ArrayList<String>> allExprs = new ArrayList<>();
    private int exprsOnLastIteration = 0;
    private ArrayList<ArrayList<String>> newIterationExprs = new ArrayList<>();

    public static void main(String[] args) throws CloneNotSupportedException {
        Lexer l = new Lexer();
        l.lex(args[0]);
        //System.out.println("\nРазбитие на лексемы:" + Lexer.seq_lexemes);

        Parser p = new Parser();
        p.parse();

        ResolutionMethod interpreter = new ResolutionMethod();

        System.out.println("\nЗадача после стадии парсинга:");
        interpreter.printStartExpressions();

        System.out.println("\nБез тождеств:");
        interpreter.deleteIdentitiesInExpr();
        interpreter.printStartExpressions();

        System.out.println("\nБез импликаций:");
        interpreter.deleteImplicationsInExpr();
        interpreter.printStartExpressions();

        System.out.println("\nПосле проноса отрицания до атомов:");
        interpreter.applyInversionsInExpr();
        interpreter.printStartExpressions();

        System.out.println("\nПосле применения закона дистрибутивности (приведение к КНФ):");
        interpreter.transformExprByDistributivityLaw();
        interpreter.printStartExpressions();

        System.out.println("\nРазбитие формул в КНФ на дизъюнкты:");
        interpreter.splitCnfExprIntoDisjuncts();
        interpreter.printStartExpressions();

        System.out.println("\nПриведение дизъюнктов к правильным дизъюнктам (удаляем повторения атомов):");
        interpreter.transformDisjunctsToRightDisjuncts();
        interpreter.printAllExpressions();

        System.out.println("\nМетод резолюции. Создание новых дизъюнктов:");
        interpreter.resolutionMethod();
    }

    private void resolutionMethod() {
        while (true) {
            int algIterationStatus = makeNewResolvents();
            //0 for no-changes, 1 for have-changes, 2 for found-solution

            if (algIterationStatus == 0)
                System.out.println("Решение не найдено. Невозможно получить новую резольвенту.");

            if (algIterationStatus == 0 || algIterationStatus == 2)
                break;

            System.out.println("-------------------------");

            if (allExprs.size() > 10000) {
                System.out.println("Решение не найдено. Бесконечная генерация дизъюнктов.");
                break;
            }
        }
    }

    private int makeNewResolvents() {
        int algIterationStatus = 0; //0 for no-changes, 1 for have-changes, 2 for found-solution

        for (int i = 0; i < allExprs.size(); i++) {
            int j = getIndexOfSecondVariable(i);

            for (; j < allExprs.size(); j++) {
                ArrayList<String> variables = new ArrayList<>();
                variables.addAll(allExprs.get(i));
                variables.addAll(allExprs.get(j));

                ArrayList<String> answerVariables = new ArrayList<>();

                int numberOfContraryPairs = findDuplicatesAndDeleteContraryVariables(variables, answerVariables, 1);

                if (numberOfContraryPairs == 1) {
                    if (answerVariables.size() == 0) {
                        algIterationStatus = 2;
                        System.out.println("Решение найдено. Получен пустой дизъюнкт из " + (i + 1) + " и " + (j + 1) + ".");
                        break;
                    } else {
                        algIterationStatus = 1;
                        printExpression(allExprs.size() + newIterationExprs.size() + 1, answerVariables);
                        System.out.println(" из " + (i + 1) + " и " + (j + 1));
                        newIterationExprs.add(answerVariables);
                    }
                }
            }

            if (algIterationStatus == 2)
                break;
        }

        exprsOnLastIteration = allExprs.size();
        allExprs.addAll(newIterationExprs);
        newIterationExprs = new ArrayList<>();

        return algIterationStatus;
    }

    private int getIndexOfSecondVariable(int indexOfFirstVariable) {
        if (exprsOnLastIteration == 0 || exprsOnLastIteration <= indexOfFirstVariable) {
            return indexOfFirstVariable + 1;
        } else {
            return exprsOnLastIteration;
        }
    }

    private void printExpression(int number, ArrayList<String> variables) {
        System.out.print(number + ") ");

        boolean isFirst = true;
        for (String var : variables) {
            if (isFirst) {
                isFirst = false;
                System.out.print(var);
            } else
                System.out.print(" | " + var);
        }
    }

    private void transformDisjunctsToRightDisjuncts() {
        for (LogicExpression expr : logicExpressions) {
            ArrayList<String> variables = new ArrayList<>(List.of(expr.printExpr(false).split(" \\| ")));
            ArrayList<String> answerVariables = new ArrayList<>();
            int numberOfContraryPairs = findDuplicatesAndDeleteContraryVariables(variables, answerVariables, 0);

            if (numberOfContraryPairs == 0)
                allExprs.add(answerVariables);
        }
    }

    private int findDuplicatesAndDeleteContraryVariables
            (ArrayList<String> variables, ArrayList<String> answerVariables, int maxNumberOfContraryPairs) {

        boolean[] ignoredVariablesMask = new boolean[variables.size()];
        int numberOfContraryPairs = 0;

        for (int i = 0; i < variables.size(); i++) {
            for (int j = i + 1; j < variables.size(); j++) {
                if (!ignoredVariablesMask[i] && !ignoredVariablesMask[j]) {
                    if (isSameVariables(variables.get(i), variables.get(j))) {
                        ignoredVariablesMask[i] = true;
                    } else if (isContraryVariables(variables.get(i), variables.get(j))) {
                        ignoredVariablesMask[i] = true;
                        ignoredVariablesMask[j] = true;
                        numberOfContraryPairs++;
                    }
                }
            }
        }

        if (numberOfContraryPairs <= maxNumberOfContraryPairs) {
            for (int i = 0; i < variables.size(); i++) {
                if (!ignoredVariablesMask[i])
                    answerVariables.add(variables.get(i));
            }
        }

        return numberOfContraryPairs;
    }

    private boolean isSameVariables(String var1, String var2) {
        return var1.equals(var2);
    }

    private boolean isContraryVariables(String var1, String var2) {
        return var1.equals("~" + var2) || var2.equals("~" + var1);
    }

    private void splitCnfExprIntoDisjuncts() {
        ArrayList<LogicExpression> newLogicExpressions = new ArrayList<>();
        for (LogicExpression expr : logicExpressions) {
            newLogicExpressions.addAll(splitIntoDisjuncts(expr));
        }
        logicExpressions = newLogicExpressions;
    }

    private ArrayList<LogicExpression> splitIntoDisjuncts(LogicExpression expr) {
        ArrayList<LogicExpression> newLogicExpressions = new ArrayList<>();

        if (expr.getOperation() == DomainTag.CONJUNCTION) {
            newLogicExpressions.addAll(splitIntoDisjuncts(expr.getFirstExpr()));
            newLogicExpressions.addAll(splitIntoDisjuncts(expr.getSecondExpr()));
        } else {
            newLogicExpressions.add(expr);
        }

        return newLogicExpressions;
    }

    private void transformExprByDistributivityLaw() {
        for (LogicExpression expr : logicExpressions) {
            transformByDistributivityLaw(expr);
        }
    }

    private void transformByDistributivityLaw(LogicExpression expr) {
        if (expr.getVariable() != null)
            return;

        if (expr.getFirstExpr() != null)
            transformByDistributivityLaw(expr.getFirstExpr());
        transformByDistributivityLaw(expr.getSecondExpr());

        if (expr.getOperation() == DomainTag.DISJUNCTION) {
            LogicExpression firstExpr = expr.getFirstExpr();
            LogicExpression secondExpr = expr.getSecondExpr();

            if (secondExpr.getOperation() == DomainTag.CONJUNCTION) {
                expr.setOperation(DomainTag.CONJUNCTION);
                expr.setFirstExpr(new LogicExpression(firstExpr, DomainTag.DISJUNCTION, secondExpr.getFirstExpr()));
                expr.setSecondExpr(new LogicExpression(firstExpr, DomainTag.DISJUNCTION, secondExpr.getSecondExpr()));

                transformByDistributivityLaw(expr);
            } else if (firstExpr.getOperation() == DomainTag.CONJUNCTION) {
                expr.setOperation(DomainTag.CONJUNCTION);
                expr.setFirstExpr(new LogicExpression(firstExpr.getFirstExpr(), DomainTag.DISJUNCTION, secondExpr));
                expr.setSecondExpr(new LogicExpression(firstExpr.getSecondExpr(), DomainTag.DISJUNCTION, secondExpr));

                transformByDistributivityLaw(expr);
            }
        }
    }

    private void applyInversionsInExpr() {
        for (LogicExpression expr : logicExpressions) {
            applyInversion(expr);
        }
    }

    private void applyInversion(LogicExpression expr) {
        if (expr.getVariable() != null)
            return;

        if (expr.getFirstExpr() != null)
            applyInversion(expr.getFirstExpr());
        applyInversion(expr.getSecondExpr());

        if (expr.getOperation() == DomainTag.INVERSION && expr.getSecondExpr().getVariable() == null) {
            LogicExpression secondExpr = expr.getSecondExpr();

            if (secondExpr.getOperation() == DomainTag.CONJUNCTION) {
                expr.setOperation(DomainTag.DISJUNCTION);
                expr.setFirstExpr(new LogicExpression(DomainTag.INVERSION, secondExpr.getFirstExpr()));
                expr.setSecondExpr(new LogicExpression(DomainTag.INVERSION, secondExpr.getSecondExpr()));
            } else if (secondExpr.getOperation() == DomainTag.DISJUNCTION) {
                expr.setOperation(DomainTag.CONJUNCTION);
                expr.setFirstExpr(new LogicExpression(DomainTag.INVERSION, secondExpr.getFirstExpr()));
                expr.setSecondExpr(new LogicExpression(DomainTag.INVERSION, secondExpr.getSecondExpr()));
            } else {
                if (secondExpr.getSecondExpr().getVariable() == null) {
                    expr.setOperation(secondExpr.getSecondExpr().getOperation());
                    expr.setFirstExpr(secondExpr.getSecondExpr().getFirstExpr());
                    expr.setSecondExpr(secondExpr.getSecondExpr().getSecondExpr());
                } else {
                    expr.setOperation(null);
                    expr.setFirstExpr(null);
                    expr.setSecondExpr(null);
                    expr.setVariable(secondExpr.getSecondExpr().getVariable());
                }
            }

            applyInversion(expr);
        }
    }

    private void deleteImplicationsInExpr() {
        for (LogicExpression expr : logicExpressions) {
            deleteImplication(expr);
        }
    }

    private void deleteImplication(LogicExpression expr) {
        if (expr.getVariable() != null)
            return;

        if (expr.getFirstExpr() != null)
            deleteImplication(expr.getFirstExpr());
        deleteImplication(expr.getSecondExpr());

        if (expr.getOperation() == DomainTag.IMPLICATION) {
            LogicExpression firstExpr = expr.getFirstExpr();

            expr.setOperation(DomainTag.DISJUNCTION);
            expr.setFirstExpr(new LogicExpression(DomainTag.INVERSION, firstExpr));
        }
    }

    private void deleteIdentitiesInExpr() {
        for (LogicExpression expr : logicExpressions) {
            deleteIdentity(expr);
        }
    }

    private void deleteIdentity(LogicExpression expr) {
        if (expr.getVariable() != null)
            return;

        if (expr.getFirstExpr() != null)
            deleteIdentity(expr.getFirstExpr());
        deleteIdentity(expr.getSecondExpr());

        if (expr.getOperation() == DomainTag.IDENTITY) {
            LogicExpression firstExpr = expr.getFirstExpr();
            LogicExpression secondExpr = expr.getSecondExpr();

            expr.setOperation(DomainTag.CONJUNCTION);
            expr.setFirstExpr(new LogicExpression(new LogicExpression(DomainTag.INVERSION, firstExpr),
                    DomainTag.DISJUNCTION, secondExpr));
            expr.setSecondExpr(new LogicExpression(firstExpr,
                    DomainTag.DISJUNCTION, new LogicExpression(DomainTag.INVERSION, secondExpr)));
        }
    }

    private void printAllExpressions() {
        int i = 1;
        for (ArrayList<String> expr : allExprs) {
            System.out.print(i + ") ");
            i++;
            boolean isFirst = true;
            for (String var : expr) {
                if (isFirst) {
                    isFirst = false;
                    System.out.print(var);
                } else
                    System.out.print(" | " + var);
            }

            System.out.println();
        }
    }

    public void printStartExpressions() {
        for (LogicExpression logicExpression : logicExpressions) {
            System.out.println("- " + logicExpression.printExpr(false));
        }
    }
}
