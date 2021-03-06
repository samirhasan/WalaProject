/*
 * This file is part of the Polyglot extensible compiler framework.
 *
 * Copyright (c) 2000-2006 Polyglot project group, Cornell University
 * Copyright (c) 2006 IBM Corporation
 * 
 */

package polyglot.main;

import polyglot.frontend.*;
import polyglot.frontend.Compiler;
import polyglot.util.ErrorInfo;
import polyglot.util.ErrorQueue;
import polyglot.util.StdErrorQueue;
import polyglot.util.QuotedStringTokenizer;

import java.io.*;
import java.util.*;

/** Main is the main program of the extensible compiler. It should not
 * need to be replaced.
 */
public class Main
{

  /** Source files specified on the command line */
  private Set<String> source;

  public final static String verbose = "verbose";

  /* modifies args */
  protected ExtensionInfo getExtensionInfo(List<String> args) throws TerminationException {
      ExtensionInfo ext = null;

      for (Iterator<String> i = args.iterator(); i.hasNext(); ) {
          String s = i.next();
          if (s.equals("-ext") || s.equals("-extension"))
          {
              if (ext != null) {
                  throw new TerminationException("only one extension can be specified");
              }

              i.remove();
              if (!i.hasNext()) {
                  throw new TerminationException("missing argument");
              }
              String extName = i.next();
              i.remove();
              ext = loadExtension("polyglot.ext." + extName + ".ExtensionInfo");
          }
          else if (s.equals("-extclass"))
          {
              if (ext != null) {
                  throw new TerminationException("only one extension can be specified");
              }

              i.remove();
              if (!i.hasNext()) {
                  throw new TerminationException("missing argument");
              }
              String extClass = i.next();
              i.remove();
              ext = loadExtension(extClass);
          }
      }
      if (ext != null) {
          return ext;
      }
      return loadExtension("polyglot.frontend.JLExtensionInfo");
  }

  public void start(String[] argv) throws TerminationException {
      start(argv, null, null);
  }

  public void start(String[] argv, ExtensionInfo ext) throws TerminationException {
      start(argv, ext, null);
  }

  public void start(String[] argv, ErrorQueue eq) throws TerminationException {
      start(argv, null, eq);
  }

  public void start(String[] argv, ExtensionInfo ext, ErrorQueue eq) throws TerminationException {
      source = new LinkedHashSet<String>();
      
      List<String> args = explodeOptions(argv);
      if (ext == null) {
          ext = getExtensionInfo(args);
      }
      
      Options options = ext.getOptions();

      try {
          argv = args.toArray(new String[0]);
          options.parseCommandLine(argv, source);
      }
      catch (UsageError ue) {
          PrintStream out = (ue.exitCode==0 ? System.out : System.err);
          if (ue.getMessage() != null && ue.getMessage().length() > 0) {
              out.println(ext.compilerName() +": " + ue.getMessage());
          }
          options.usage(out);
          throw new TerminationException(ue.exitCode);
      }

      if (eq == null) {
          eq = new StdErrorQueue(System.err,
                                 options.error_count,
                                 ext.compilerName());
      }

      Compiler compiler = new Compiler(ext, eq);
      Globals.initialize(compiler);

      long time0 = System.currentTimeMillis();

      if (!compiler.compileFiles(source)) {
          throw new TerminationException(1);
      }

      if (Report.should_report(verbose, 1)) {
          reportTime("Total time=" + (System.currentTimeMillis() - time0), 1);
      }
  }

  private List<String> explodeOptions(String[] args) throws TerminationException {
      LinkedList<String> ll = new LinkedList<String>();

      for (int i = 0; i < args.length; i++) {
          // special case for the @ command-line parameter
          if (args[i].startsWith("@")) {
              String fn = args[i].substring(1);
              try {
                  BufferedReader lr = new BufferedReader(new FileReader(fn));
                  LinkedList<String> newArgs = new LinkedList<String>();

                  while (true) {
                      String l = lr.readLine();
                      if (l == null)
                          break;

                      StringTokenizer st = new StringTokenizer(l, " ");
                      while (st.hasMoreTokens())
                          newArgs.add(st.nextToken());
                  }

                  lr.close();
                  ll.addAll(newArgs);
              }
              catch (java.io.IOException e) {
                  throw new TerminationException("cmdline parser: couldn't read args file "+fn);
              }
              continue;
          }

          ll.add(args[i]);
      }

      return ll;
  }

  public static void main(String args[]) {
      try {
          new Main().start(args);
      }
      catch (TerminationException te) {
          if (te.getMessage() != null)
              (te.exitCode==0?System.out:System.err).println(te.getMessage());
          System.exit(te.exitCode);
      }
  }

  static ExtensionInfo loadExtension(String ext) throws TerminationException {
    if (ext != null && ! ext.equals("")) {
      Class extClass = null;

      try {
        extClass = Class.forName(ext);
      }
      catch (ClassNotFoundException e) {
          throw new TerminationException(
            "Extension " + ext +
            " not found: could not find class " + ext + ".");
      }

      try {
        return (ExtensionInfo) extClass.newInstance();
      }
      catch (ClassCastException e) {
          throw new TerminationException(
	    ext + " is not a valid polyglot extension:" +
	    " extension class " + ext +
	    " exists but is not a subclass of ExtensionInfo");
      }
      catch (Exception e) {
          throw new TerminationException(
	           "Extension " + ext +
	           " could not be loaded: could not instantiate " + ext + ".");
      }
    }
    return null;
  }

  static private Collection<String> timeTopics = new ArrayList<String>(1);
  static {
      timeTopics.add("time");
  }

  static private void reportTime(String msg, int level) {
      Report.report(level, msg);
  }

  /**
   * This exception signals termination of the compiler. It should be used
   * instead of <code>System.exit</code> to allow Polyglot to be called
   * within a JVM that wasn't started specifically for Polyglot, e.g. the
   * Apache ANT framework.
   */
  public static class TerminationException extends RuntimeException {
      final public int exitCode;
      public TerminationException(String msg) {
          this(msg, 1);
      }
      public TerminationException(int exit) {
          this.exitCode = exit;
      }
      public TerminationException(String msg, int exit) {
          super(msg);
          this.exitCode = exit;
      }
  }
}
