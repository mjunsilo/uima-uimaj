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

package org.apache.uima.cas;

import java.util.Iterator;
import java.util.NoSuchElementException;

/** 
 * Iterator over feature structures.
 * 
 * <p>This iterator interface extends {@link java.util.Iterator java.util.Iterator}, and supports the 
 * standard <code>hasNext</code> and <code>next</code> methods.  
 * If finer control, including reverse iteration, is needed, see below.
 *  
 * <p>The <code>FSIterator</code> interface introduces the methods {@link #get()}, 
 * {@link #moveToNext()}, {@link #moveToPrevious()} methods.  With these methods, retrieving the 
 * current element (<code>get</code>) is a separate operation from moving the iterator 
 * (<code>moveToNext</code> and <code>moveToPrevious</code>.  This makes the user's code less compact,
 * but allows for finer control.
 * 
 * <p>Specifically the <code>get</code> method is defined to return the same element that
 * a call to <code>next()</code> would return, but does not advance the iterator.
 * 
 * <p>Implementations of this interface are not required to be fail-fast.  That
 * is, if the iterator's collection is modified, the effects on the iterator
 * are in general undefined.  Some collections may handle this more gracefully
 * than others, but in general, concurrent modification of the collection you're
 * iterating over is a bad idea.
 * 
 * <p>If the iterator is moved past the boundaries of the collection, the
 * behavior of subsequent calls to {@link FSIterator#moveToNext() moveToNext()} or
 * {@link FSIterator#moveToPrevious() moveToPrevious()} is undefined.  For example, if a
 * previously valid iterator is invalidated by a call to {@link FSIterator#moveToNext() moveToNext()},
 * a subsequent call to {@link FSIterator#moveToPrevious() moveToPrevious()} is not guaranteed
 * to set the iterator back to the last element in the collection.  Always use
 * {@link FSIterator#moveToLast() moveToLast()} in such cases.
 *
 *  
 *  
 */
public interface FSIterator extends Iterator {
  
  /**
   * Check if this iterator is valid.
   * @return <code>true</code> iff the iterator is valid.
   */
  boolean isValid();
  
  /**
   * Get the structure the iterator is pointing at.
   * @return The structure the iterator is pointing at.
   * @exception NoSuchElementException If the iterator is not valid.
   */
  FeatureStructure get() throws NoSuchElementException;
  
  /**
   * Advance the iterator.  This may invalidate the iterator.
   */
  void moveToNext();
  
  /**
   * Move the iterator one element back.  This may invalidate the iterator.
   */
  void moveToPrevious();
  
  /**
   * Move the iterator to the first element.  The iterator will be valid iff
   * the underlying collection is non-empty.
   */
  void moveToFirst();
  
  /**
   * Move the iterator to the last element.  The iterator will be valid iff
   * the underlying collection is non-empty.
   */
  void moveToLast();
  
  /**
   * Move the iterator to the first features structure that is equal to
   * <code>fs</code>.  If no such feature structure exists in the underlying
   * collection, set the iterator to the "insertion point" for <code>fs</code>,
   * i.e., to a point where the current feature structure is greater than
   * <code>fs</code>, and the previous one is less than <code>fs</code>.
   * @param fs The feature structure the iterator should be set to.
   */
  void moveTo(FeatureStructure fs);
  
  /**
   * Copy this iterator.
   * @return A copy of this iterator, pointing at the same element.
   */
  FSIterator copy();

}
