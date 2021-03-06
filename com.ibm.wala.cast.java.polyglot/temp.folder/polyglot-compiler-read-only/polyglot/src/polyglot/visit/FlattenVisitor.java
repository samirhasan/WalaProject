/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package polyglot.visit;

import java.util.*;

import polyglot.ast.*;
import polyglot.types.*;

/**
 * The FlattenVisitor flattens the AST,
 */
public class FlattenVisitor extends NodeVisitor
{
    protected TypeSystem ts;
    protected NodeFactory nf;
    protected LinkedList stack;

    public FlattenVisitor(TypeSystem ts, NodeFactory nf) {
	this.ts = ts;
	this.nf = nf;
	stack = new LinkedList();
    }

    public Node override(Node parent, Node n) {
	// Insert Blocks when needed to allow local decls to be inserted.
	if (parent instanceof Stmt && n instanceof Stmt) {
	    Stmt s1 = (Stmt) n;
	    if (! (s1 instanceof Block)) {
		Stmt s2 = nf.Block(s1.position(), s1);
		return visitEdgeNoOverride(parent, s2);
	    }
	}

	if (n instanceof FieldDecl || n instanceof ConstructorCall) {
            if (! stack.isEmpty()) {
                List l = (List) stack.getFirst();
                l.add(n);
            }
	    return n;
	}

        // punt on switch statement
        if (n instanceof Switch) {
            return n;
        }
                

        if (neverFlatten.contains(n)) {
            return n;
        }

        if (n instanceof ArrayInit) {
            return n;
        }

	return null;
    }

    protected static int count = 0;

    protected static Name newID() {
	return Name.makeFresh("tmp");
    }

    protected Set noFlatten = new HashSet();
    protected Set neverFlatten = new HashSet();

    /** 
     * When entering a BlockStatement, place a new StatementList
     * onto the stack
     */
    public NodeVisitor enter(Node parent, Node n) {
	if (n instanceof Block) {
	    stack.addLast(new LinkedList());
	}
	
	// Don't flatten the expression contained in the statement, but
	// flatten its subexpressions.
	if (parent instanceof Stmt && n instanceof Expr) {
	    noFlatten.add(n);
	}

	if (parent instanceof Assign) {
	    noFlatten.add(n);
	}

	return this;
    }

    /** 
     * Flatten complex expressions within the AST
     */
    public Node leave(Node parent, Node old, Node n, NodeVisitor v) {
        if (noFlatten.contains(old)) {
	    noFlatten.remove(old);
	    return n;
	}

	if (n instanceof Block) {
	    List l = (List) stack.removeFirst();
            Block block = ((Block) n).statements(l);
            if (parent instanceof Block && !stack.isEmpty()) {
              l = (List) stack.getFirst();
              l.add(block);
            }
	    return block;
	}
	else if (n instanceof Stmt) {
	    List l = (List) stack.getFirst();
	    l.add(n);
	    return n;
	}
	else if (n instanceof Expr && ! (n instanceof Lit) &&
	      ! (n instanceof Special) && ! (n instanceof Local)) {

	    Expr e = (Expr) n;

	    if (e instanceof Assign) {
	        return n;
	    }

            /*
            if (e.isTypeChecked() && e.type().isVoid()) {
                return n;
            }
            */

	    // create a local temp, initialized to the value of the complex
	    // expression

	    Name name = newID();
	    LocalDecl def = nf.LocalDecl(e.position(), nf.FlagsNode(e.position(), Flags.FINAL),
					 nf.CanonicalTypeNode(e.position(),
					                      Types.<Type>ref(e.type())),
					 nf.Id(e.position(), name), e);
	    LocalDef li = ts.localDef(e.position(), Flags.FINAL,
        		     Types.<Type>ref(e.type()), name);
	    def = def.localDef(li);

	    List l = (List) stack.getFirst();
	    l.add(def);

	    // return the local temp instead of the complex expression
	    Local use = nf.Local(e.position(), nf.Id(e.position(), name));
	    use = (Local) use.type(e.type());
	    use = use.localInstance(li.asInstance());
	    return use;
	}

	return n;
    }
}
