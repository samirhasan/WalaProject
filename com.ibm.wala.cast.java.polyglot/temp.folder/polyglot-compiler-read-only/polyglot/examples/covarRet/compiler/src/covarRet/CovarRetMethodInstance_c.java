package polyglot.ext.covarRet;

import polyglot.ext.jl.types.*;
import polyglot.types.*;
import polyglot.util.*;
import java.util.*;

/**
 * A <code>MethodInstance</code> represents the type information for a Java
 * method.
 */
public class CovarRetMethodInstance_c extends MethodInstance_c
{
    /** Used for deserializing types. */
    protected CovarRetMethodInstance_c() { }

    public CovarRetMethodInstance_c(TypeSystem ts, Position pos,
	 		    ReferenceType container,
	                    Flags flags, Type returnType, String name,
			    List argTypes, List excTypes) {
        super(ts, pos, container, flags, returnType, name, argTypes, excTypes);
    }

    public boolean canOverrideImpl(MethodInstance mj, boolean quiet) throws SemanticException {
        MethodInstance mi = this;

        // This is the only rule that has changed.
        if (! ts.isSubtype(mi.returnType(), mj.returnType())) {
        	if (quiet) return false;
            throw new SemanticException(mi.signature() + " in " + mi.container() +
                    " cannot override " + 
                    mj.signature() + " in " + mj.container() + 
                    "; incompatible " +
                    "return types",
                    mi.position());
        } 

        // Force the return types to be the same and then let the super
        // class perform the remainder of the tests.
        MethodInstance tmpMj = (MethodInstance) mj.copy();
        tmpMj.setReturnType(mi.returnType());
        return super.canOverrideImpl(tmpMj, quiet);
    }
}
