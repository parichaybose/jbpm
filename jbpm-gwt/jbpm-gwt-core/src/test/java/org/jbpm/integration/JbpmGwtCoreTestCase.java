package org.jbpm.integration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import junit.framework.Assert;

import org.drools.SystemEventListenerFactory;
import org.drools.persistence.util.PersistenceUtil;
import org.jbpm.task.Group;
import org.jbpm.task.User;
import org.jbpm.task.service.TaskService;
import org.jbpm.task.service.TaskServiceSession;
import org.jbpm.task.service.hornetq.HornetQTaskServer;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a.. wacky test (suite). 
 * 
 * ! Adding an @AfterClass method will mess up the tests. 
 */
public abstract class JbpmGwtCoreTestCase extends Assert {

    private static Logger logger = LoggerFactory.getLogger(JbpmGwtCoreTestCase.class);
    
	private static HashMap<String, Object> context = null;
	private static EntityManagerFactory emf;
	
	static HornetQTaskServer minaServer;
	static Thread minaServerThread;
    
    @BeforeClass
	public static void setUp(){
        if( context == null ) { 
            context = PersistenceUtil.setupWithPoolingDataSource(PersistenceUtil.JBPM_PERSISTENCE_UNIT_NAME, false);
        }
    	if (minaServerThread==null){
    	    System.setProperty("jbpm.console.directory","./src/test/resources");
    	    startHumanTaskServer();
		}
	
	}

	protected static  void startHumanTaskServer(){
		emf = Persistence.createEntityManagerFactory("org.jbpm.task");
        TaskService taskService = new TaskService(emf, SystemEventListenerFactory.getSystemEventListener());
        TaskServiceSession taskSession = taskService.createSession();
    
        // Add users
        Map vars = new HashMap();
        Reader reader;
		try {
			reader = new FileReader(new File("./src/test/resources/LoadUsers.mvel"));
		
        Map<String, User> users = ( Map<String, User> ) eval( reader, vars );   
        for ( User user : users.values() ) {
        	taskSession.addUser( user );
        	
        }           
        reader = new FileReader(new File("./src/test/resources/LoadGroups.mvel"));      
        Map<String, Group> groups = ( Map<String, Group> ) eval( reader, vars );     
        for ( Group group : groups.values() ) {
            taskSession.addGroup( group );
        }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
        // start server
        minaServer = new HornetQTaskServer(taskService, 5445);
        minaServerThread = new Thread(minaServer);
        minaServerThread.start();
        taskSession.dispose();
        
        logger.debug("Task service started correctly !");
        logger.debug("Task service running ...");
	}
 	
	@SuppressWarnings("rawtypes")
	public static Object eval(Reader reader, Map vars) {
	    return TaskService.eval(reader, vars);
    }
    
    public static String readerToString(Reader reader) throws IOException {
        int charValue = 0;
        StringBuffer sb = new StringBuffer( 1024 );
        while ( (charValue = reader.read()) != -1 ) {
            //result = result + (char) charValue;
            sb.append( (char) charValue );
        }
        return sb.toString();
    }


}



