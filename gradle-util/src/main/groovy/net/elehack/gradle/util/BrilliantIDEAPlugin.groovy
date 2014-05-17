/*
 * Gradle utilities
 * Copyright 2014 Michael Ekstrand
 * Derived from the LensKit Build System:
 *     Copyright 2010-2014 Regents of the University of Minnesota and contributors
 *     Work on LensKit has been funded by the National Science Foundation under
 *     grants IIS 05-34939, 08-08692, 08-12148, and 10-17697.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * - Neither the name of the University of Minnesota nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.elehack.gradle.util

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.GradleBuild

/**
 * Extended IDEA plugin. Apply this to your root project only.  It will recursively handle
 * all subprojects.  If you have a 'buildSrc' project, its build file must apply the 'idea'
 * plugin.
 */
class BrilliantIDEAPlugin implements Plugin<Project> {
    @Override
    void apply(Project prj) {
        prj.apply plugin: 'idea'
        def iprTask = prj.tasks['ideaProject']
        prj.subprojects {
            apply plugin: 'idea'
            iprTask dependsOn ideaModule
        }
        prj.allprojects { sp ->
            afterEvaluate {
                idea {
                    module {
                        if (plugins.hasPlugin('base')) {
                            outputDir = sourceSets.main.output.classesDir
                            testOutputDir = sourceSets.test.output.classesDir
                        }
                        downloadSources = true
                        downloadJavadoc = true
                        inheritOutputDirs = false
                    }
                }
            }
        }
        def bsDir = prj.file('buildSrc')
        if (bsDir.exists()) {
            def bsIml = prj.task('ideaBuildSrcModule', type: GradleBuild) {
                dir bsDir
                tasks = ['ideaModule']
            }
            iprTask.dependsOn bsIml
        }

        prj.idea {
            project {
                ipr {
                    withXml {
                        Node ipr = it.asNode()
                        Node modules = ipr.depthFirst().find {
                            it.name() == 'modules'
                        }
                        def bsm = modules.children().find { Node kid ->
                            kid.attribute('fileurl').endsWith('buildSrc.iml')
                        }
                        if (bsm == null && bsDir.exists()) {
                            logger.info 'adding buildSrc module'
                            def attrs = [
                                    fileurl: 'file://$PROJECT_DIR$/buildSrc/buildSrc.iml',
                                    filepath: '$PROJECT_DIR$/buildSrc/buildSrc.iml'
                            ]
                            modules.appendNode('module', attrs)
                        }

                        ipr.children().each { Node comp ->
                            if (comp.name() == 'component' && comp.attribute('name') == 'ProjectRootManager') {
                                def output = comp.get('output')
                                if (output != null) {
                                    output.@url = 'file://$PROJECT_DIR$/build/classes'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
