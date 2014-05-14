package test;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import dm.Component;
import dm.ComponentState;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.context.Event;
import dm.impl.ComponentImpl;
import dm.impl.ConfigurationDependencyImpl;
import dm.impl.DependencyImpl;
import dm.impl.EventImpl;

/**
 * This test class simulates a client having many dependencies being registered/unregistered concurrently.
 */
public class ServiceRaceTest extends TestBase {
    final static int STEP_WAIT = 5000;
    final static int DEPENDENCIES = 10;
    final static int LOOPS = 10000;

    // Executor used to bind/unbind service dependencies.
    ExecutorService m_threadpool;
    // Timestamp used to log the time consumed to execute 100 tests.
    long m_timeStamp;

    /**
     * Creates many service dependencies, and activate/deactivate them concurrently.  
     */
    @Test
    public void createParallelComponentRegistgrationUnregistration() {
        info("Starting createParallelComponentRegistgrationUnregistration test");
        int cores = Math.max(16, Runtime.getRuntime().availableProcessors());
        info("using " + cores + " cores.");

        m_threadpool = Executors.newFixedThreadPool(Math.max(cores, DEPENDENCIES + 3 /* start/stop/configure */));

        try {
            m_timeStamp = System.currentTimeMillis();
            for (int loop = 0; loop < LOOPS; loop++) {
                doTest(loop);
            }
        }
        catch (Throwable t) {
            warn("got unexpected exception", t);
        }
        finally {
            shutdown(m_threadpool);
        }
    }

    void shutdown(ExecutorService exec) {
        exec.shutdown();
        try {
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
        }
    }

    void doTest(int loop) throws Throwable {
        debug("loop#%d -------------------------", loop);

        final Ensure step = new Ensure(false);

        // Create one client component, which depends on many service dependencies
        final ComponentImpl client = new ComponentImpl();
        final Client theClient = new Client(step);
        client.setImplementation(theClient);

        // Create client service dependencies
        final DependencyImpl[] dependencies = new DependencyImpl[DEPENDENCIES];
        for (int i = 0; i < DEPENDENCIES; i++) {
            dependencies[i] = new DependencyImpl();
            dependencies[i].setRequired(true);
            dependencies[i].setCallbacks("add", "remove");
            client.add(dependencies[i]);
        }
        final ConfigurationDependencyImpl confDependency = new ConfigurationDependencyImpl();
        client.add(confDependency);

        // Create Configuration (concurrently).
        // We have to simulate the configuration update, using a component state listener, which will
        // trigger an update thread, but only once the component is started.
        final ComponentStateListener listener = new ComponentStateListener() {
            private volatile Dictionary m_conf;

            public void changed(Component c, ComponentState state) {
                if (state == ComponentState.WAITING_FOR_REQUIRED && m_conf == null) {
                    m_conf = new Hashtable();
                    m_conf.put("foo", "bar");
                    m_threadpool.execute(new Runnable() {
                        public void run() {
                            try {
                                confDependency.updated(m_conf);
                            }
                            catch (ConfigurationException e) {
                                warn("configuration failed", e);
                            }
                        }
                    });
                }
            }
        };
        client.add(listener);

        // Activate the client service dependencies concurrently.
        for (int i = 0; i < DEPENDENCIES; i++) {
            final DependencyImpl dep = dependencies[i];
            final Event added = new EventImpl(i);
            m_threadpool.execute(new Runnable() {
                public void run() {
                    dep.add(added);
                }
            });
        }

        // Start the client (concurrently)
        m_threadpool.execute(new Runnable() {
            public void run() {
                client.start();
            }
        });

        // Ensure that client has been started.
        int expectedStep = 1 /* conf */ + DEPENDENCIES + 1 /* start */;
        step.waitForStep(expectedStep, STEP_WAIT);
        Assert.assertEquals(DEPENDENCIES, theClient.getDependencies());
        Assert.assertNotNull(theClient.getConfiguration());
        client.remove(listener);

        // Stop the client and all dependencies concurrently.
        for (int i = 0; i < DEPENDENCIES; i++) {
            final DependencyImpl dep = dependencies[i];
            final Event removed = new EventImpl(i);
            m_threadpool.execute(new Runnable() {
                public void run() {
                    dep.remove(removed);
                }
            });
        }
        m_threadpool.execute(new Runnable() {
            public void run() {
                client.stop();
            }
        });
        m_threadpool.execute(new Runnable() {
            public void run() {
                try {
                    confDependency.updated(null);
                }
                catch (ConfigurationException e) {
                    warn("error while unconfiguring", e);
                }
            }
        });

        // Ensure that client has been stopped, then destroyed, then unbound from all dependencies
        expectedStep += 2; // stop/destroy
        expectedStep += DEPENDENCIES; // removed all dependencies
        expectedStep += 1; // removed configuration
        step.waitForStep(expectedStep, STEP_WAIT);
        step.ensure();
        Assert.assertEquals(0, theClient.getDependencies());
        Assert.assertNull(theClient.getConfiguration());

        debug("finished one test loop");
        if ((loop + 1) % 100 == 0) {
            long duration = System.currentTimeMillis() - m_timeStamp;
            warn("Performed 100 tests (total=%d) in %d ms.", (loop + 1), duration);
            m_timeStamp = System.currentTimeMillis();
        }
    }

    public class Client {
        final Ensure m_step;
        int m_dependencies;
        volatile Dictionary m_conf;
        
        public Client(Ensure step) {
            m_step = step;
        }

        public void updated(Dictionary conf) throws ConfigurationException {
            m_conf = conf;
            if (conf != null) {
                Assert.assertEquals("bar", conf.get("foo"));
                m_step.step(1);
            } else {
                m_step.step();
            }
        }
        
        synchronized void add() {
            m_step.step();
            m_dependencies++;
        }
        
        synchronized void remove() {
            m_step.step();
            m_dependencies--;
        }
                
        void start() {
            m_step.step((DEPENDENCIES + 1) /* deps + conf */ + 1 /* start */);
        }

        void stop() {
            m_step.step((DEPENDENCIES + 1) /* deps + conf */ + 1 /* start */ + 1 /* stop */);
        }
        
        void destroy() {
            m_step.step((DEPENDENCIES + 1) /* deps + conf */ + 1 /* start */ + 1 /* stop */  + 1 /* destroy */);
        }
        
        synchronized int getDependencies() {
            return m_dependencies;
        }
        
        Dictionary getConfiguration() {
            return m_conf;
        }
    }
}
