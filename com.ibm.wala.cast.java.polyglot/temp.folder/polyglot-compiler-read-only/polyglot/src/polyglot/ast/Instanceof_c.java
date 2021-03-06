/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2007 Polyglot project group, Cornell University
 * Copyright (c) 2006-2007 IBM Corporation
 * 
 */

package polyglot.ast;

import java.util.List;

import polyglot.types.*;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.*;

/**
 * An <code>Instanceof</code> is an immutable representation of
 * the use of the <code>instanceof</code> operator.
 */
public class Instanceof_c extends Expr_c implements Instanceof
{
    protected Expr expr;
    protected TypeNode compareType;

    public Instanceof_c(Position pos, Expr expr, TypeNode compareType) {
	super(pos);
	assert(expr != null && compareType != null);
	this.expr = expr;
	this.compareType = compareType;
    }

    /** Get the precedence of the expression. */
    public Precedence precedence() {
	return Precedence.INSTANCEOF;
    }

    /** Get the expression to be tested. */
    public Expr expr() {
	return this.expr;
    }

    /** Set the expression to be tested. */
    public Instanceof expr(Expr expr) {
	Instanceof_c n = (Instanceof_c) copy();
	n.expr = expr;
	return n;
    }

    /** Get the type to be compared against. */
    public TypeNode compareType() {
	return this.compareType;
    }

    /** Set the type to be compared against. */
    public Instanceof compareType(TypeNode compareType) {
	Instanceof_c n = (Instanceof_c) copy();
	n.compareType = compareType;
	return n;
    }

    /** Reconstruct the expression. */
    protected Instanceof_c reconstruct(Expr expr, TypeNode compareType) {
	if (expr != this.expr || compareType != this.compareType) {
	    Instanceof_c n = (Instanceof_c) copy();
	    n.expr = expr;
	    n.compareType = compareType;
	    return n;
	}

	return this;
    }

    /** Visit the children of the expression. */
    public Node visitChildren(NodeVisitor v) {
	Expr expr = (Expr) visitChild(this.expr, v)  ;
	TypeNode compareType = (TypeNode) visitChild(this.compareType, v);
	return reconstruct(expr, compareType);
    }

    /** Type check the expression. */
    public Node typeCheck(ContextVisitor tc) throws SemanticException {
        TypeSystem ts = tc.typeSystem();

        if (! compareType.type().isReference()) {
	    throw new SemanticException(
		"Type operand " + compareType.type() + " must be a reference type.",
		compareType.position());
	}

	if (! ts.isCastValid(expr.type(), compareType.type(), tc.context())) {
	    throw new SemanticException(
		"Expression operand type " + expr.type() + " incompatible with type operand " + compareType.type() + ".",
		expr.position());
	}

	return type(ts.Boolean());
    }

    public Type childExpectedType(Expr child, AscriptionVisitor av) {
        TypeSystem ts = av.typeSystem();

        if (child == expr) {
            return ts.Object();
        }

        return child.type();
    }

    public String toString() {
	return expr + " instanceof " + compareType;
    }

    /** Write the expression to an output file. */
    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	printSubExpr(expr, w, tr);
	w.write(" instanceof ");
	print(compareType, w, tr);
    }

    public Term firstChild() {
        return expr;
    }

    public List<Term> acceptCFG(CFGBuilder v, List<Term> succs) {
        v.visitCFG(expr, compareType, ENTRY);
        v.visitCFG(compareType, this, EXIT);
        return succs;
    }
    

}
