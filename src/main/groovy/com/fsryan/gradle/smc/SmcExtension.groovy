package com.fsryan.gradle.smc

/**
 * Created by ryan on 2/21/17.
 */
class SmcExtension {
    /**
     * <p>
     *     The URI of the Smc.jar file. For example, if your Smc.jar file is on your local hard drive, then
     *     you should use file://path/to/Smc.jar. If the Smc.jar is hosted on some (preferably internal) website,
     *     then use https://some_url.com/Smc.jar
     * </p>
     * <p>
     *     In order have more efficient local builds, you should:
     *     <ol>
     *         <li>
     *             download the zip
     *         </li>
     *         <li>
     *             unzip it yourself
     *         </li>
     *         <li>
     *             create a local property with the file path of the Smc.jar file
     *         </li>
     *         <li>
     *             Assign the value of that local property to this smcUri (ensure that file:// is prefixed for local
     *             files)
     *         </li>
     *     </ol>
     * </p>
     * <p>
     *     Should that file not exist or the download of the file from the url fail, the default behavior will take over
     *     The default behavior:
     *     <ol>
     *         <li>
     *             downloads a zip of the latest smc hosted by sourceforge
     *         </li>
     *         <li>
     *             unzips it
     *         </li>
     *         <li>
     *             runs the Smc.jar
     *         </li>
     *         <li>
     *             deletes the zip file and the unzipped directory
     *          </li>
     *      </ol>
     *      However, this is mainly for out-of-the-box behavior to work properly, and there is no good reason to
     *      continue downloading the zip file for each build
     * </p>
     */
    String smcUri

    /**
     * <p>
     *     The URI of the statemap.jar file. This works exactly as {@link #smcUri} does except that the statemap.jar
     *     file is intended to be a dependency of your app.
     * </p>
     */
    String statemapJarUri

    /**
     * <p>
     *     Defaults to "libs". If the directory named does not exist, and {@link #statemapJarUri}
     *     is true, then the directory will be created. However, if you have not added the .jar files in
     *     this directory to the classpath via dependencies, then the statemap.jar will not be added to the
     *     classpath.
     * </p>
     */
    String libsDirectory = "libs"

    /**
     * <p>
     *     Defaults to "sm". This directory is relative to each of your source sets. For example, the default
     *     source set 'main' would have main/java and main/sm. Equivalently, you can have test/sm or debug/sm or
     *     release/sm, however, it's debatable whether this would be valuable
     * </p>
     * <p>
     *     You should not configure sm as a property of any sourceSet. The plugin just assumes that, as source,
     *     your state machine's DSL files could be treated just like any other source in your gradle build.
     * </p>
     * <p>
     *     Your state machine's DSL file should be in a directory in keeping with the java package structure. So
     *     if you want to have MyStateMachine.java generated in the com.my.state.machine package of the main java
     *     source set, then the the DSL file for it be located here:
     *     src/main/sm/com/my/state/machine/MyStateMachine.sm
     * </p>
     */
    String smSrcDir = "sm"

    /**
     * <p>
     *     Defaults to -1. If you want to generate the .dot (graphviz) file in your ${buildDir}/outputs/${smSrcDir}*
     *     directory so that you can generate graphs using the graphviz application, then make this 0 or positive
     * </p>
     * <p>
     *     Possible values:
     *     <ul>
     *         <li>
     *             &lt; 0: do not generate a graph
     *         </li>
     *         <li>
     *             0: generate a graph with state names, transition names, and pop transition nodes
     *         </li>
     *         <li>
     *             1: same as 0, but adding entry/exit actions, transition guards, and transition actions
     *         </li>
     *         <li>
     *             2: same as 1, but adding entry/exit action, pop transition, and transition action arguments as well
     *             as transition parameters
     *         </li>
     *         <li>
     *             &gt; 2: same as 2
     *         </li>
     *     </ul>
     * <p>
     *     Installation of graphviz is system-dependent. Therefore, no attempt is made to transform this .dot file
     *     into a .png or .pdf . . . yet.
     * </p>
     */
    int graphVizLevel = -1

    /**
     * <p>
     *     Defaults to false. If you want to generate the .html ile in your ${buildDir}/outputs/${smSrcDir}*
     *     directory so that you can view a table of all the states, actions, transitions, and guards in your browser,
     *     then flip this to true
     * </p>
     */
    boolean outputHtmlTable = false
}
