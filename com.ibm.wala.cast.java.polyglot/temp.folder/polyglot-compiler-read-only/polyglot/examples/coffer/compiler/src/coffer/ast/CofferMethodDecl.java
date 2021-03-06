/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * 
 */

package coffer.ast;

import polyglot.ast.*;
import java.util.*;

/** An immutable representation of the Coffer method declaration.
 * <code>ConstructorDecl</code> is extended with pre- and post-conditions.
 */
public interface CofferMethodDecl extends MethodDecl {
    KeySetNode entryKeys();
    CofferMethodDecl entryKeys(KeySetNode entryKeys);
    
    KeySetNode returnKeys();
    CofferMethodDecl returnKeys(KeySetNode returnKeys);

    List throwConstraints();
    CofferMethodDecl throwConstraints(List throwConstraints);
}
