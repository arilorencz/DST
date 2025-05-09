package dst.ass2.ioc.tests.di;

import dst.ass2.ioc.di.IObjectContainer;
import dst.ass2.ioc.di.IObjectContainerFactory;
import dst.ass2.ioc.di.annotation.Component;
import dst.ass2.ioc.di.annotation.Inject;
import dst.ass2.ioc.di.impl.ObjectContainerFactory;
import dst.ass2.ioc.tests.grading.GitHubClassroomGrading;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class ObjectContainerGradingTest {

    private IObjectContainerFactory factory;
    private IObjectContainer container;

    @Before
    public void setUp() throws Exception {
        factory = new ObjectContainerFactory();
        container = factory.newObjectContainer(new Properties());
    }

    @Component
    public static class DeepClassA {
        @Inject
        HierarchyTest.SimpleSingleton singletonA;
    }

    @Component
    public static class DeepClassB extends DeepClassA {
        @Inject
        HierarchyTest.SimpleSingleton singletonB;
    }

    @Component
    public static class DeepClassC extends DeepClassB {
        @Inject
        HierarchyTest.SimpleSingleton singletonC;
    }

    @Component
    public static class DeepClassD extends DeepClassC {
        @Inject
        HierarchyTest.SimpleSingleton singletonD;
    }

    /**
     * Tests the following hierarchy:
     * [D] -is a-> [C] -is a-> [B] -is a-> [A]
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 3)
    public void getObject_initializesDeepHierarchyCorrectly() throws Exception {
        DeepClassD d = container.getObject(DeepClassD.class);

        assertNotNull("same level should be initialized", d.singletonD);
        assertNotNull("one level up should be initialized", d.singletonC);
        assertNotNull("two levels up should be initialized", d.singletonB);
        assertNotNull("three levels up should be initialized", d.singletonA);
    }

    @Component
    public static class SingletonA {
        @Inject
        DependencyInjectionTest.SimplePrototype prototypeA;
    }

    @Component
    public static class SingletonB {
        @Inject
        DependencyInjectionTest.SimplePrototype prototypeB;
    }

    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 4)
    public void getObject_onDifferentSingletons_hasDifferentPrototypeMembers() throws Exception {
        SingletonA singletonA = container.getObject(SingletonA.class);
        SingletonB singletonB = container.getObject(SingletonB.class);

        assertSame(singletonA.prototypeA.getClass(), singletonB.prototypeB.getClass());
        assertNotSame(singletonA.prototypeA, singletonB.prototypeB);
    }


    @Component
    public static class ClassA {
        @Inject
        HierarchyTest.SimpleSingleton singletonA;
    }

    @Component
    public static class ClassB extends ClassA {

        @Inject
        HierarchyTest.SimpleSingleton singletonB;

        @Inject
        ClassD classD;
    }

    @Component
    public static class ClassC {

        @Inject
        HierarchyTest.SimpleSingleton singletonC;
    }

    @Component
    public static class ClassD extends ClassC {

        @Inject
        HierarchyTest.SimpleSingleton singletonD;
    }

    /**
     * Tests the following hierarchy. Getting first B then D should should use the previously in B injected D.
     * <p>
     * +-------+      +-------+
     * |   A   |      |   C   |
     * +-------+      +-------+
     * ^ is a         ^ is a
     * |              |
     * +-------+ uses +-------+
     * |   B   | ---> |   D   |
     * +-------+      +-------+
     */
    @Test(timeout = 2000)
    @GitHubClassroomGrading(maxScore = 2)
    public void getObject_initializesHierarchyCorrectly_inversed() throws Exception {
        ClassB b = container.getObject(ClassB.class);

        assertNotNull("getObject returned null for ClassB", b);
        assertNotNull("ClassB dependency was not injected", b.singletonB);
        assertNotNull("ClassA dependency was not injected when instantiating ClassB", b.singletonA);
        assertNotNull("ClassD dependency was not injected into ClassB", b.classD);

        ClassD d = container.getObject(ClassD.class);

        assertNotNull("getObject returned null for ClassD", d);
        assertNotNull("ClassD dependency was not injected", d.singletonD);
        assertNotNull("ClassC dependency was not injected when instantiating ClassD", d.singletonC);

        assertSame("Container did not re-use already initialized ClassD instance", b.classD, d);
    }


    @Ignore("check thread safety of singleton management")
    @Test
    public void getObject_isThreadSafeTowardsCreatingSingletons() throws Exception {
        // check manually
    }

    @Ignore("check that at least two custom non-trivial tests have been developed")
    @Test
    public void check_CustomTestsImplemented() throws Exception {
        //check manually
    }

}
