package gov.loc.repository.service.container;

import javax.annotation.PostConstruct;

import gov.loc.repository.service.component.ComponentFactory;
import gov.loc.repository.service.component.ComponentInvoker;
import gov.loc.repository.serviceBroker.RespondingServiceBroker;
import gov.loc.repository.serviceBroker.ServiceRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component("serviceContainer")
@ManagedResource(objectName="bean:name=serviceContainer")
public class ServiceContainer implements Runnable {
	
	public enum State {STARTING, STARTED, STOPPING, STOPPED, SHUTTINGDOWN, SHUTDOWN};
	
	private static final Log log = LogFactory.getLog(ServiceContainer.class);

	private ComponentFactory factory;
	private ThreadPoolTaskExecutor executor;
	private Long wait = 10000L;
	private State state = State.STOPPED;
	private RespondingServiceBroker broker;
	private ServiceContainerHeartbeat heartbeat;
	
	public ServiceContainer(ThreadPoolTaskExecutor executor, RespondingServiceBroker broker, ComponentFactory factory, ServiceContainerHeartbeat registry) {
		this.factory = factory;
		this.executor = executor;
		this.executor.setWaitForTasksToCompleteOnShutdown(true);
		this.broker = broker;
		this.heartbeat = registry;
	}
	
	public void setWait(Long wait)
	{
		this.wait = wait;
	}
	
	@ManagedAttribute
	public String[] getJobTypes()
	{
		return this.broker.getJobTypes();
	}
	
	@ManagedAttribute
	public String[] getQueues()
	{
		return this.broker.getQueues();
	}
	
	@ManagedAttribute
	public String getResponder()
	{
		return this.broker.getResponder();
	}

	@ManagedAttribute
	public Integer getActiveServiceRequestCount()
	{
		return this.executor.getActiveCount();
	}
	
	@ManagedAttribute
	public Long getMaxMemory()
	{
		return Runtime.getRuntime().maxMemory()/1024;		
	}

	@ManagedAttribute
	public Long getTotalMemory()
	{
		return Runtime.getRuntime().totalMemory()/1024;		
	}
	
	@ManagedAttribute
	public Long getFreeMemory()
	{
		return Runtime.getRuntime().freeMemory()/1024;		
	}
	
	
	@PostConstruct
	public void init() {		
		for(String jobType : this.broker.getJobTypes())
		{
			log.debug("Checking if component factory handles jobType: " + jobType);
			if (! this.factory.handlesJobType(jobType))
			{
				throw new UnsupportedOperationException("Factory cannot create component to handle jobType " + jobType);
			}
		}
		
		for(String queue : this.broker.getQueues())
		{
			log.debug("Handles queue: " + queue);
		}
		
		//Report any uncompleted tasks for this responder as errors
		this.broker.reportErrorsForAcknowledgedServiceRequestsWithoutResponses();
	}

	public void shutdown()
	{
		log.debug("Shutting down");
		this.state = State.SHUTTINGDOWN;
		this.run();
	}
	
	public void run()
	{
		if (this.state == State.STARTING)
		{
			this.state = State.STARTED;
			log.debug("Starting");
			this.heartbeat.start();
		}
		
		while(this.state != State.SHUTDOWN)
		{
			if (this.state == State.STARTED)
			{
				try
				{				
					ServiceRequest req = this.getNextServiceRequest();
					while(req != null)
					{					
						this.executor.execute(new ServiceRunnable(req, this.broker, this.factory));					
						req = this.getNextServiceRequest();
					}
					try
					{
						log.debug("Sleeping");
						Thread.sleep(this.wait);
						log.debug("Done sleeping");
					}
					catch(InterruptedException ex)
					{
						log.warn("Thread sleeping interrupted", ex);
					}
				}
				catch(Exception ex)
				{
					log.error(ex);
					this.stop();
				}
			}
			else if (this.state == State.STOPPING || this.state == State.SHUTTINGDOWN)
			{
				this.executor.shutdown();
								
				if (this.state == State.STOPPING)
				{
					log.debug("Stopped");
					this.state = State.STOPPED;
				}
				else
				{
					this.state = State.SHUTDOWN;
				}
			}
		}
		this.heartbeat.stop();		
		log.debug("Shutdown");
	}
	
	@ManagedOperation
	public void start()
	{
		if (this.state == State.STOPPED)
		{
			this.state = State.STARTING;
			(new Thread(this)).start();
		}
		else
		{
			log.warn("Could not start because state is " + this.state);
		}
	}

	private ServiceRequest getNextServiceRequest()
	{
		if (! this.threadsAreAvailable())
		{
			log.debug("No threads are available");
			return null;
		}
		return this.broker.findAndAcknowledgeNextServiceRequest();
	}
	
	private boolean threadsAreAvailable()
	{
		return this.executor.getActiveCount() < this.executor.getMaxPoolSize();
	}
		
	public State getState()
	{
		return this.state;
	}
	
	@ManagedAttribute
	public String getStateString()
	{
		return this.state.toString();
	}
	
	@ManagedOperation
	public void stop()
	{
		log.debug("Stopping");		
		this.state = State.STOPPING;
	}
		
	public class ServiceRunnable implements Runnable
	{
		private ServiceRequest req;
		private RespondingServiceBroker broker;
		private ComponentFactory componentFactory;
		
		public ServiceRunnable(ServiceRequest req, RespondingServiceBroker broker, ComponentFactory factory) {
			this.req = req;
			this.broker = broker;
			this.componentFactory = factory;
		}
		
		public ServiceRequest getServiceRequest()
		{
			return this.req;
		}
		
		@Override
		public void run() {			
			Object component = null;
			try {
				component = componentFactory.getComponent(req.getJobType());
			} catch (Exception ex) {
				req.respondFailure(ex);
			}
			if (component != null)
			{
				ComponentInvoker helper = new ComponentInvoker();
				//Invoke and return taskResult
				log.info("Received request: " + req);
				System.out.println("Starting " + req);
				helper.invoke(component, req);
			}
			
			log.info("Responding to request: " + req);
			System.out.println("Responding " + req);
			broker.sendResponse(req);
								
		}
	}
	
	
}
