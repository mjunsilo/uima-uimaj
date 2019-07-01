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

package org.apache.uima;

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.uima.internal.util.I18nUtil;

/**
 * The <code>InternationalizedRuntimeException</code> class adds internationalization support to
 * the standard functionality provided by <code>java.lang.RuntimeException</code>. Because this
 * is a <code>RuntimeException</code>, it does not need to be declared in the throws clause of
 * methods.
 * <p>
 * To support internationalization, the thrower of an exception must not specify a hardcoded message
 * string. Instead, the thrower specifies a key that identifies the message. That key is then looked
 * up in a locale-specific {@link java.util.ResourceBundle ResourceBundle} to find the actual
 * message associated with this exception.
 * <p>
 * This class also supports arguments to messages. The full message will be constructed using the
 * {@link java.text.MessageFormat MessageFormat} class. For more information on
 * internationalization, see the <a href="http://java.sun.com/j2se/1.4/docs/guide/intl/index.html">
 * Java Internationalization Guide</a>.
 * <p>
 * This version of this class works with JDK versions prior to 1.4, since it does not assume support
 * for exception chaining. The file <code>InternationalizedException.java_1_4</code> is a version
 * that uses the exception chaining support built-in to JDK1.4.
 * 
 * 
 */
public class InternationalizedRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 6387360855459370559L;

  /**
   * The base name of the resource bundle in which the message for this exception is located.
   */
  private String mResourceBundleName;

  /**
   * An identifier that maps to the message for this exception.
   */
  private String mMessageKey;

  /**
   * The arguments to this exception's message, if any. This allows an
   * <code>InternationalizedRuntimeException</code> to have a compound message, made up of
   * multiple parts that are concatenated in a language-neutral way.
   */
  private Object[] mArguments;

  /**
   * The exception that caused this exception to occur.
   */
  private Throwable mCause;

  /**
   * the thread local class loader at creation time, see UIMA-4793
   * Transient to allow exceptions to be serialized.
   * Deserialized versions have null as their value, which is handled by the users
   */
  final transient private ClassLoader originalContextClassLoader;

  // see https://issues.apache.org/jira/browse/UIMA-5961
  // the resourceBundle associated with the default locale, at the time of creation of this instance
  final transient private ResourceBundle default_localized_resourceBundle;
  // the default locale, at the time of creation of this instance
  final transient private Locale default_locale;
  // a user specified resource bundle, used when the default_locale is not appropriate
  transient private ResourceBundle user_specified_resourceBundle = null;
  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with a null message.
   */
  public InternationalizedRuntimeException() {
    this(null, null, null, null);
  }

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with the specified cause and a
   * null message.
   * 
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public InternationalizedRuntimeException(Throwable aCause) {
    this(null, null, null, aCause);
  }

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with the specified message.
   * 
   * @param aResourceBundleName
   *          the base name of the resource bundle in which the message for this exception is
   *          located.
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   */
  public InternationalizedRuntimeException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments) {
    this(aResourceBundleName, aMessageKey, aArguments, null);
  }

  /**
   * Creates a new <code>InternationalizedRuntimeException</code> with the specified message and
   * cause.
   * 
   * @param aResourceBundleName
   *          the base name of the resource bundle in which the message for this exception is
   *          located.
   * @param aMessageKey
   *          an identifier that maps to the message for this exception. The message may contain
   *          placeholders for arguments as defined by the
   *          {@link java.text.MessageFormat MessageFormat} class.
   * @param aArguments
   *          The arguments to the message. <code>null</code> may be used if the message has no
   *          arguments.
   * @param aCause
   *          the original exception that caused this exception to be thrown, if any
   */
  public InternationalizedRuntimeException(String aResourceBundleName, String aMessageKey,
          Object[] aArguments, Throwable aCause) {
    super();
    originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      I18nUtil.setTccl(originalContextClassLoader); 
      default_locale = Locale.getDefault();
      default_localized_resourceBundle = (aMessageKey == null) 
          ? null 
          : I18nUtil.resolveResourceBundle(aResourceBundleName, default_locale, null);      
    } finally {
      I18nUtil.removeTccl();        
    }
    mCause = aCause;
    mResourceBundleName = aResourceBundleName;
    mMessageKey = aMessageKey;
    mArguments = aArguments;
    // if null message and mCause is Internationalized exception, "promote" message
    if (mResourceBundleName == null && mMessageKey == null) {
      if (mCause instanceof InternationalizedException) {
        mResourceBundleName = ((InternationalizedException) mCause).getResourceBundleName();
        mMessageKey = ((InternationalizedException) mCause).getMessageKey();
        mArguments = ((InternationalizedException) mCause).getArguments();
      } else if (mCause instanceof InternationalizedRuntimeException) {
        mResourceBundleName = ((InternationalizedRuntimeException) mCause).getResourceBundleName();
        mMessageKey = ((InternationalizedRuntimeException) mCause).getMessageKey();
        mArguments = ((InternationalizedRuntimeException) mCause).getArguments();
      }
    }
  }

  /**
   * Gets the base name of the resource bundle in which the message for this exception is located.
   * 
   * @return the resource bundle base name. May return <code>null</code> if this exception has no
   *         message.
   */
  public String getResourceBundleName() {
    return mResourceBundleName;
  }

  /**
   * Gets the identifier for this exception's message. This identifier can be looked up in this
   * exception's {@link java.util.ResourceBundle ResourceBundle} to get the locale-specific message
   * for this exception.
   * 
   * @return the resource identifier for this exception's message. May return <code>null</code> if
   *         this exception has no message.
   */
  public String getMessageKey() {
    return mMessageKey;
  }

  /**
   * Gets the arguments to this exception's message. Arguments allow a
   * <code>InternationalizedRuntimeException</code> to have a compound message, made up of
   * multiple parts that are concatenated in a language-neutral way.
   * 
   * @return the arguments to this exception's message.
   */
  public Object[] getArguments() {
    if (mArguments == null)
      return new Object[0];

    Object[] result = new Object[mArguments.length];
    System.arraycopy(mArguments, 0, result, 0, mArguments.length);
    return result;
  }

  /**
   * Gets the <i>English</i> detail message for this exception. For the localized message use
   * {@link #getLocalizedMessage()}.
   * 
   * @return the English detail message for this exception.
   */
  public String getMessage() {
    return getLocalizedMessage(Locale.ENGLISH);
  }

  /**
   * Gets the localized detail message for this exception. This uses the default Locale for this
   * JVM. A Locale may be specified using {@link #getLocalizedMessage(Locale)}.
   * 
   * @return this exception's detail message, localized for the default Locale.
   */
  public String getLocalizedMessage() {
    return getLocalizedMessage(Locale.getDefault());
  }

  /**
   * Gets the localized detail message for this exception using the specified <code>Locale</code>.
   * 
   * @param aLocale
   *          the locale to use for localizing the message
   * 
   * @return this exception's detail message, localized for the specified <code>Locale</code>.
   */
  public String getLocalizedMessage(Locale aLocale) {
    // check for null message
    if (getMessageKey() == null)
      return null;

    if (default_localized_resourceBundle != null && aLocale == default_locale) {
      return I18nUtil.localizeMessage(default_localized_resourceBundle, aLocale, getMessageKey(), getArguments());
    }
    
    if (user_specified_resourceBundle != null) {
      return I18nUtil.localizeMessage(user_specified_resourceBundle, aLocale, getMessageKey(), getArguments());
    }
    
    try {
      I18nUtil.setTccl(originalContextClassLoader);
      return I18nUtil.localizeMessage(getResourceBundleName(), aLocale, getMessageKey(), getArguments());
    } finally {
      I18nUtil.removeTccl();        
    }   
//      // locate the resource bundle for this exception's messages
//      ResourceBundle bundle = ResourceBundle.getBundle(getResourceBundleName(), aLocale);
//      // retrieve the message from the resource bundle
//      String message = bundle.getString(getMessageKey());
//      // if arguments exist, use MessageFormat to include them
//      if (getArguments().length > 0) {
//        MessageFormat fmt = new MessageFormat(message);
//        fmt.setLocale(aLocale);
//        return fmt.format(getArguments());
//      } else
//        return message;
//    } catch (Exception e) {
//      return "EXCEPTION MESSAGE LOCALIZATION FAILED: " + e.toString();
//    }
  }

  /**
   * Gets the cause of this Exception.
   * 
   * @return the Throwable that caused this Exception to occur, if any. Returns <code>null</code>
   *         if there is no such cause.
   */
  public Throwable getCause() {
    return mCause;
  }

  public synchronized Throwable initCause(Throwable cause) {
    mCause = cause;
    return this;
  }

  /**
   * For the case where the default locale is not being used for getting messages,
   * and the lookup path in the classpath for the resource bundle needs to be set 
   * at a specific point, call this method to set the resource bundle at that point in the call stack.
   * 
   * Example: If in a Pear, and you are throwing an exception, which is defined in a bundle
   * in the Pear context, but the catcher of the throw is up the stack above where the pear context
   * exists (and therefore, is no longer present at "catch" time), and
   * you don't want to use the default-locale for getting the message out of the message bundle,
   * 
   * then do something like this
   *   Exception e = new AnalysisEngineProcessException(MESSAGE_BUNDLE, "TEST_KEY", objects);
   *   e.setResourceBundle(my_locale);  // call this method, pass in the needed locale object
   *   throw e;  // or whatever should be done with it
   * @param aLocale the locale to use when getting the message from the message bundle at a later time
   */
  public void setResourceBundle(Locale aLocale) {
    user_specified_resourceBundle = I18nUtil.resolveResourceBundle(mResourceBundleName, aLocale, null);
  }

}
