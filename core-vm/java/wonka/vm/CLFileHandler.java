/**************************************************************************
* Copyright (c) 2001 by Punch Telematix. All rights reserved.             *
*                                                                         *
* Redistribution and use in source and binary forms, with or without      *
* modification, are permitted provided that the following conditions      *
* are met:                                                                *
* 1. Redistributions of source code must retain the above copyright       *
*    notice, this list of conditions and the following disclaimer.        *
* 2. Redistributions in binary form must reproduce the above copyright    *
*    notice, this list of conditions and the following disclaimer in the  *
*    documentation and/or other materials provided with the distribution. *
* 3. Neither the name of Punch Telematix nor the names of                 *
*    other contributors may be used to endorse or promote products        *
*    derived from this software without specific prior written permission.*
*                                                                         *
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED          *
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF    *
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.    *
* IN NO EVENT SHALL PUNCH TELEMATIX OR OTHER CONTRIBUTORS BE LIABLE       *
* FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR            *
* CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF    *
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR         *
* BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,   *
* WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE    *
* OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN  *
* IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                           *
**************************************************************************/

/*
** $Id: CLFileHandler.java,v 1.3 2006/05/16 08:24:41 cvs Exp $
*/

package wonka.vm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

public class CLFileHandler extends ClassLoaderURLHandler {

  private File directory;

  CLFileHandler(File file){
    directory = file;
  }

  public byte[] getByteArray(String resource){
    try {
      InputStream in =  new FileInputStream(new File(directory, resource));
      int len_avail = in.available();
      int len_read = 0;
      byte[] bytes = new byte[len_avail];
      while (len_read < len_avail) {
        int l = in.read(bytes,0,len_avail-len_read);
	if (l < 0) {
          return null;
	}
	len_read += l;
      }

      /**
      ** we might need to check if we got all data ...
      */

      return bytes;
    } catch(Exception e){}

    return null;
  }

  public InputStream getInputStream(String resource){
    try {
      return new FileInputStream(new File(directory, resource));
    } catch(Exception e){}

    return null;
  }

  public URL getURL(String resource){
    try {
      File f = new File(directory, resource);
      if(f.isFile()){
        return f.toURL();
      }
    } catch(Exception e){}

    return null;
  }

  public String toString() {
    return "CLFileHandler for directory " + directory;
  }
}
