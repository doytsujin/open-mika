/**************************************************************************
 * Copyright  (c) 2001 by Acunia N.V. All rights reserved.                 *
 *                                                                         *
 * This software is copyrighted by and is the sole property of Acunia N.V. *
 * and its licensors, if any. All rights, title, ownership, or other       *
 * interests in the software remain the property of Acunia N.V. and its    *
 * licensors, if any.                                                      *
 *                                                                         *
 * This software may only be used in accordance with the corresponding     *
 * license agreement. Any unauthorized use, duplication, transmission,     *
 *  distribution or disclosure of this software is expressly forbidden.    *
 *                                                                         *
 * This Copyright notice may not be removed or modified without prior      *
 * written consent of Acunia N.V.                                          *
 *                                                                         *
 * Acunia N.V. reserves the right to modify this software without notice.  *
 *                                                                         *
 *   Acunia N.V.                                                           *
 *   Vanden Tymplestraat 35      info@acunia.com                           *
 *   3000 Leuven                 http://www.acunia.com                     *
 *   Belgium - EUROPE                                                      *
 **************************************************************************/

package java.security.spec;

public class X509EncodedKeySpec extends EncodedKeySpec {

  public X509EncodedKeySpec(byte[] encodedKey) {
    super(encodedKey);
  }

  public byte[] getEncoded() {
    return super.getEncoded();
  }

  public final String getFormat() {
    return "X.509";
  }

}
