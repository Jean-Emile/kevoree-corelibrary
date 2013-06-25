package org.kevoree.library.frascati.mavenplugin;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.kevoree.ContainerRoot;
import org.kevoree.KevoreeFactory;
import org.kevoree.framework.KevoreeXmiHelper;
import org.kevoree.impl.DefaultKevoreeFactory;

/**
 * Created by IntelliJ IDEA.
 * User: duke
 * Date: 30/01/12
 * Time: 17:33
 */

/**
 * @author ffouquet
 * @author <a href="mailto:ffouquet@irisa.fr">Fouquet François</a>
 * @version $Id$
 * @goal compile
 * @phase process-classes
 * @requiresDependencyResolution compile
 */
public class KevoreeFrascatiMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     *
     * @parameter default-value="${project.basedir}"
     */
    private File basedir;

    /**
     *
     * @parameter default-value="${project.basedir}/src/main/resources"
     */
    private File resources;

    
    /**
    *
    * @parameter default-value="${project.basedir}/target/classes"
    */
   private File targetresources;


    public void execute() throws MojoExecutionException, MojoFailureException {

    	List<File> res = new ArrayList<File>();
    	listFile(resources,res);
  	    ContainerRoot root = new DefaultKevoreeFactory().createContainerRoot();
  	    for (File f : res){
  	    	org.kevoree.library.frascati.mavenplugin.CompositeParser.parseCompositeFile(root,f,project.getVersion(),project.getGroupId(),project.getArtifactId(),f.getName());
    	}
  	    File resdir = new File(targetresources.getAbsolutePath() + File.separatorChar + "KEV-INF");
  	    resdir.mkdir();
  	    File resFile = new File(targetresources.getAbsolutePath() + File.separatorChar + "KEV-INF" + File.separatorChar + "lib.kev" );
    	KevoreeXmiHelper.instance$.save(resFile.getAbsolutePath(), root);
    	System.err.println(resFile.getAbsolutePath());
    	
    	
    }
    
    
    public void listFile(File dir, Collection<File> files) {
		GenericExtFilter filter = new GenericExtFilter("composite");
		// list out all the file name and filter by the extension
		File[] list = dir.listFiles(filter);
        if(list == null){
            return ;
        }


		for (File file : list) {
			files.add(file);
		}
		GenericDirFilter dirfilter = new GenericDirFilter();
		File[] dirs = dir.listFiles(dirfilter);
		for (File d : dirs) {
			listFile(d, files);
		}
    }
    
    class GenericExtFilter implements FileFilter {
		private String ext;
		public GenericExtFilter(String ext) {
			this.ext = ext;
		}
		@Override
		public boolean accept(File arg0) {
			return 	arg0.getName().endsWith(ext);
		}
	}
    class GenericDirFilter implements FileFilter {
		@Override
		public boolean accept(File arg0) {
			return 	arg0.isDirectory();
		}
	}

}

    
    
