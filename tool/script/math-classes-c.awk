#############################################################################
# Copyright (c) 2005 by Chris Gray, trading as /k/ Embedded Java Solutions. #
# All rights reserved.  The contents of this file may not be copied or      #
# distributed in any form without express written consent of the author.    #
#############################################################################

#
# $Id: math-classes-c.awk,v 1.2 2006/06/09 09:51:06 cvs Exp $
# 
# The Wonka kernel is software copyright by SmartMove NV (1999).
# Please see the file Copyright for information on it's legal use.
# 
#  First output banner, #incudes, and a couple of utility functions

BEGIN {
  print " "
  print "/*"
  print "** This file is generated automatically from math-classes.in"
  print "** Do NOT edit this file."
  print "*/"
  print " "
  print "#include \"wstrings.h\""
  print "#include \"core-classes.h\""
  print "#include \"clazz.h\""
  print "#include \"fields.h\""
  print "#include \"hashtable.h\""
  print "#include \"jni.h\""
  print "#include \"locks.h\""
  print "#include \"methods.h\""
  print "#include \"ts-mem.h\""
  print "#include \"threads.h\""
  print "#include \"loading.h\""
  print "#include \"math-classes.h\""
  print " "
  print "extern w_instance extensionClassLoader;"
  print " "

}

# skip lines beginning with '#'

/^\#/{next}

# No leading whitespace -> clazz declaration
# two or more fields -> field or method

/^[a-zA-Z]/ {
  ++count

  fqn=$1
  slash=index(fqn,"/")
  lastslash=0
  while(slash) { lastslash+=slash; rest=substr(fqn,lastslash+1); slash=index(rest,"/") }
  thisclazz=substr(fqn,lastslash+1)
  gsub("\\$", "_dollar_", thisclazz)
  clazz[count]=thisclazz
  path[count]=substr(fqn,1,lastslash)

  l=length(thisclazz)
  if(l>4&&(l-index(thisclazz,"Error")==4)) likelyerrors=thisclazz" "likelyerrors
  if(l>8&&(l-index(thisclazz,"Exception")==8)) likelyexceptions=thisclazz" "likelyexceptions
}

/^[ \t]/ {

# If third field is "+" then second is a "wotsit" field.
# If third field begins with "(" then second was a method name
# and third is its descriptor; if present, the fourth is the native 
# function entry point.
# If the third field is neither "+" nor begins with "(" then the
# second was a field.

  thismember = $1
  gsub("\\$", "_dollar_", thismember)
  if($2=="+") {
    fictitiousfields[thisclazz] = thismember" "fictitiousfields[thisclazz]
    wotsits[thisclazz] = thismember" "wotsits[thisclazz]
  }
  else if(substr($2,1,1)!="(") {
    realfields[thisclazz,$1] = "F_"thisclazz"_"thismember
  }
  else {
    method=thismember
    descr=$2
    if($3) methods[thisclazz,method,descr] = $3
  }

}

END {
  print " "
  print "/*"
  print "** w_clazz declarations for the math classes"
  print "*/"
  print " "

  for (c = 1; c in clazz; ++c) {
    printf "w_clazz clazz%s;\n",clazz[c]
  }
  print  " "

  print "/*"
  print "** slot numbers of Java fields"
  print "*/"
  print " "

  for(cf in realfields) {
    printf "w_int %s;\n",realfields[cf]
  }
  print " "

  print "/*"
  print "** slot numbers of \"fictitious\" fields (wotsits and their ilk)"
  print "*/"
  print " "
  for(c in fictitiousfields) {
    split(fictitiousfields[c], a);
    for (i in a) {
      printf "w_int F_%s_%s;\n",c,a[i]
    }
  }
  print " "

  printf("/*\n** Wotsit functions for math-classes\n*/\n\n");
  for(c = 1; c in clazz; ++c) {
    thisclazz = clazz[c];
    mcount = 0;
    for (m in methods) {
      split(m, a, SUBSEP)
      if(a[1] == thisclazz) {
        if (!mcount) {
          printf "JNINativeMethod %s_methods[] = {\n", thisclazz
        }
        printf "  {\"%s\", \"%s\", %s},\n", a[2], a[3], methods[m]
        ++mcount;
      }
    }
    if (mcount) {
      printf("};\n\n");
    }

    split(wotsits[thisclazz], ff);
    have_fixup1 = 0;
    have_fixup2 = 0;
    for (cf in realfields) {
      split(cf, a, SUBSEP)
      if(a[1] == thisclazz) {
        if (!have_fixup1) {
          have_fixup1 = 1;
          printf "void fixup1_%s(w_clazz clazz) {\n\n", thisclazz
        }
        printf "  %s = findFieldOffset(clazz, \"%s\");\n",realfields[cf],a[2]
      }        
    }
    j = 0;
    for(i in ff) {
      j++;
    }
    if (j) {
      if (!have_fixup1) {
        have_fixup1 = 1;
        printf "void fixup1_%s(w_clazz clazz) {\n\n", thisclazz
      }
      for(i in ff) {
        printf "  woempa(1, \"clazz '%s' wotsit field '%s' at index %%d.\\n\", clazz->instanceSize - clazz->numReferenceFields);\n", thisclazz, ff[i]
        printf "#ifdef PACK_BYTE_FIELDS\n"
        printf "  F_%s_%s = FIELD_SIZE_32_BITS + (clazz->instanceSize - clazz->numReferenceFields);\n", thisclazz, ff[i]
        printf "  if (clazz->nextByteSlot == (clazz->instanceSize * 4)) {\n"
        printf "    clazz->instanceSize += 4;\n"
        printf "  }\n"
        printf "#else\n"
        printf "  F_%s_%s = clazz->instanceSize - clazz->numReferenceFields;\n", thisclazz, ff[i]
        printf "#endif\n"
        printf "  clazz->instanceSize += 1;\n"
      }
    }
    if (have_fixup1) {
      clazz_fixup1[c] = "fixup1_" thisclazz;
      printf "}\n\n"
    }
    if (mcount) {
      printf "void fixup2_%s(w_clazz clazz) {\n\n", thisclazz
      printf "  registerNatives(clazz, %s_methods, %d);\n", thisclazz, mcount
      clazz_fixup2[c] = "fixup2_" thisclazz;
      printf "}\n\n"
    }
  }

  print "void collectMathFixups() {"
  print "  w_string slashed, dotified;"
  for(c = 1; c in clazz; ++c) {
    thisclazz = clazz[c]
    gsub("_dollar_", "$", thisclazz);
    fu = clazz_fixup1[c];
    if (fu) {
      string = sprintf("%s%s", path[c], thisclazz);
      printf "        slashed = cstring2String(\"%s\",%d);\n",string,length(string)
      printf "        dotified = slashes2dots(slashed);\n"
      printf "        woempa(1,\"Adding {%%w,%%p} to fixup1_hashtable\\n\",dotified,%s);\n",fu
      printf "        deregisterString(slashed);\n"
      printf "        ht_write_no_lock(fixup1_hashtable,(w_word)dotified,(w_word)%s);\n",fu
    }
    fu = clazz_fixup2[c];
    if (fu) {
      string = sprintf("%s%s", path[c], thisclazz);
      printf "        slashed = cstring2String(\"%s\",%d);\n",string,length(string)
      printf "        dotified = slashes2dots(slashed);\n"
      printf "        woempa(1,\"Adding {%%w,%%p} to fixup2_hashtable\\n\",dotified,%s);\n",fu
      printf "        deregisterString(slashed);\n"
      printf "        ht_write_no_lock(fixup2_hashtable,(w_word)dotified,(w_word)%s);\n",fu
    }
  }
  print "}"

  print  "w_clazz loadOneMathClass(w_string name) {"
  print  "  w_string dotified;"
  print  "  w_clazz clazz;"
  print  "  dotified = slashes2dots(name);"
  print  "  clazz = seekClazzByName(dotified, NULL);"
  print  "  if (clazz == NULL) {"
  print  "    clazz = loadNonBootstrapClass(extensionClassLoader, dotified);"
  print  "    if (clazz == NULL) {"
  printf "      woempa(9,\"Unable to find WNI class %%w:\\n\",name);\n"
  print  "    }"
  print  "  }"
  print  "  deregisterString(dotified);"
  print  "  return clazz;"
  print "}"
  print ""
  print "void loadMathClasses() {"

  for(c = 1; c in clazz; ++c) {
    string = sprintf("%s%s", path[c], clazz[c]);
    thisclazz = clazz[c]
    printf "  clazz%s = loadOneMathClass(cstring2String(\"%s\", %d));\n", thisclazz, string, length(string);
  }
  printf  "\n}\n\n"

}

