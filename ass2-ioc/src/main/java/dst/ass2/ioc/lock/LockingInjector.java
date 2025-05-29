package dst.ass2.ioc.lock;

import dst.ass2.ioc.di.annotation.Component;
import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class LockingInjector implements ClassFileTransformer {

  @Override
  public byte[] transform(ClassLoader loader, String className,
                          Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) throws IllegalClassFormatException {

    System.out.println("transform");
    ClassPool pool = ClassPool.getDefault();
    pool.appendClassPath(new LoaderClassPath(loader));

    CtClass cc = null;
    byte[] tmp = Arrays.copyOf(classfileBuffer, classfileBuffer.length);
    try {
      cc = pool.makeClass(new ByteArrayInputStream(tmp));
      System.out.println("cc = " + cc.getName());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (!cc.hasAnnotation(Component.class)) {
      System.out.println("!cc.hasAnnotation(Component.class)");
      return classfileBuffer;
    }

    for (CtMethod method : cc.getDeclaredMethods()) {
      Object lockAnnotation = null;
      try {
        if (method.hasAnnotation(Lock.class)) {
          lockAnnotation = method.getAnnotation(Lock.class);
          System.out.println("lockAnnotation = " + lockAnnotation.toString());

          Lock lock = (Lock) lockAnnotation;
          String lockName = lock.value();

          System.out.println("lockName = " + lockName);

          try {
            method.insertBefore("dst.ass2.ioc.lock.LockManager.getInstance().getLock(\"" + lockName + "\").lock();");
            method.insertAfter("dst.ass2.ioc.lock.LockManager.getInstance().getLock(\"" + lockName + "\").unlock();", true);

          } catch (Exception e) {
            throw new RuntimeException("Failed to instrument method: " + method.getName(), e);
          }
        }
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    try {
      return cc.toBytecode();
    } catch (IOException | CannotCompileException e) {
      throw new RuntimeException(e);
    }
  }

}