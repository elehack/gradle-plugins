package net.elehack.gradle.science

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to initialize the gradle-science environment.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
class SciencePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('science', ScienceExtension)
    }
}