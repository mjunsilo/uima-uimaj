/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.jcas.cas;

import org.apache.uima.cas.FeatureStructure;

/**
 * This class is the super class of arrays of Feature Structures
 */
public interface CommonArray extends FeatureStructure {
  
  int size();
  
  void copyValuesFrom(CommonArray v);
  
  /**
   * Creates a new string array and copies this array's values into it.
   * 
   * @return A Java array copy of this array.
   */
  String[] toStringArray();
  
  /**
   * @return a comma-separated string of the string values of the elements of the array
   */
  default String getValuesAsCommaSeparatedString() { 
    String [] sa = toStringArray();
    StringBuilder sb = new StringBuilder();
    boolean isFirst = true;
    for (String s : sa) {
      if (!isFirst) {
        sb.append(',');
      }
      isFirst = false;
      sb.append(s);
    }
    return sb.toString();
  }
}