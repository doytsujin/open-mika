/**
 * Copyright  (c) 2006 by Chris Gray, /k/ Embedded Java Solutions.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of /k/ Embedded Java Solutions nor the names of other contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL /K/
 * EMBEDDED SOLUTIONS OR OTHER CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * $Id: BuildSettingsChecker.java,v 1.3 2006/09/20 14:21:05 cvsroot Exp $
 */
package be.kiffer.mika.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.CallTarget;

/**
 * BuildSettingsChecker:
 *
 * @author Gerrit Ruelens
 *
 * created: Sep 14, 2006
 */
public class BuildSettingsChecker extends Task {
  
  private String file;
  private String task;
  
  private final static String[] keylist = new String[] {
    "SECURITY" , "JAR", "MATH", "JAVA_BEANS", "AWT",
    "JAVAX_CRYPTO", "JAVAX_COMM", "AWT_DEF",
    "DEBUG", "STATIC", "TESTS", "JAM.PLATFORM"
  };
  
  public final String getFile() {
    return file;
  }

  public final void setFile(String file) {
    this.file = file;
  }

  public final String getTask() {
    return task;
  }

  public final void setTask(String task) {
    this.task = task;
  } 

  public void execute() throws BuildException {
    if(file == null) {
      throw new BuildException("specify a 'file'");
    }
    
    File f = new File(file);
    
    Project project = getProject();
    Hashtable props = (Hashtable) project.getProperties().clone();
    props.putAll(project.getUserProperties());

    try {
      if(f.isFile() && checkProperties(props)) {
        return;
      }
      //file doesn't exist or different.    
      writeFile(props);
    } catch (IOException e) {
      throw (BuildException) new BuildException(e.getMessage()).initCause(e);
    }
  }

  private void writeFile(Hashtable list) throws IOException {
    Properties props = new Properties();
    for(int i=0 ; i < keylist.length ; i++) {
      String key = keylist[i];
      Object value = list.get(key);
      props.setProperty(key,(value == null ? "" : (String)value));
    }
    
    log("Storing "+props+" to '"+file+"'");
    
    props.store(new FileOutputStream(file), "ANT GENERATED - DO NOT CHANGE !");    
  }

  private boolean checkProperties(Hashtable list) throws IOException {
    Properties props = new Properties();    
    props.load(new FileInputStream(file));
    for(int i=0 ; i < keylist.length ; i++) {
      String key = keylist[i];
      Object value = list.get(key);
      String setting = value == null ? "" : (String)value;
      if(!setting.equals(props.getProperty(key))) {
        log("Key '"+key+"' doesn't math:");
        log("\twas = '"+props.getProperty(key)+"'");
        log("\t is = '"+value+"'");
        CallTarget call = new CallTarget();
        call.setLocation(this.getLocation());
        call.setInheritAll(true);
        call.setProject(this.getProject());
        call.setOwningTarget(this.getOwningTarget());
        call.setDescription(task);
        call.setTaskName("/k/" + task);
        call.setTarget(task);
        call.init();
        call.execute();        
        return false;
      }
    }  
    log("All keys matched !");
    return true;    
  }
}
