package bee;

import bee.api.Repository;
import bee.task.Jar;
import kiss.I;

public class Install extends bee.task.Install {

    /**
     * Install the current Bee into your environment.
     */
    @Override
    public void project() {
        require(Jar::document, Jar::merge);

        I.make(Repository.class).install(project);

        BeeInstaller.install(project.locateJar());
    }
}