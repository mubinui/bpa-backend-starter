# Flowable BPA Service Spring Boot Integration Guide

## Introduction

This documentation provides a comprehensive guide to integrating Flowable Business Process Automation (BPA) services with a Spring Boot application. The guide is based on the implementation patterns found in the `bpa-backend-starter` project and provides practical examples for developers who want to incorporate workflow automation into their applications.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Project Setup](#project-setup)
3. [Core Components](#core-components)
4. [Workflow Integration](#workflow-integration)
5. [Event Handling System](#event-handling-system)
6. [Configuration Management](#configuration-management)
7. [API Endpoints](#api-endpoints)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)

## Architecture Overview

The integration between Spring Boot and Flowable BPA follows a service-oriented architecture pattern where:

1. **Controller Layer** - Handles HTTP requests and delegates to services
2. **Service Layer** - Contains business logic and workflow operations
3. **Client Layer** - Communicates with the Flowable BPA engine
4. **Event System** - Manages workflow events and triggers application logic

The following diagram illustrates how these components interact:

```
┌─────────────────┐     ┌───────────────────┐     ┌─────────────────────┐
│                 │     │                   │     │                     │
│  REST API       ├────►│  Service Layer    ├────►│  BPA Client         │
│  Controller     │     │                   │     │                     │
│                 │     │                   │     │                     │
└─────────────────┘     └───────┬───────────┘     └─────────┬───────────┘
                                │                           │
                                │                           │
                                ▼                           ▼
                       ┌────────────────┐          ┌────────────────────┐
                       │                │          │                    │
                       │  Business      │◄─────────┤  Workflow Event    │
                       │  Logic         │          │  Listeners         │
                       │                │          │                    │
                       └────────────────┘          └────────────────────┘
```

## Project Setup

### Dependencies

Add the following dependencies to your `build.gradle` file:

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.bracits:bpa-client-spring-boot-starter:3.0.8.0-SNAPSHOT'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

### Maven Repository Configuration

If using a private repository for the BPA client:

```gradle
repositories {
    mavenCentral()
    maven {
        url "<your-repository-url>"
        credentials(HttpHeaderCredentials) {
            name = 'Deploy-Token'
            value = '<your-token>'
        }
        authentication {
            header(HttpHeaderAuthentication)
        }
    }
}
```

### Basic Application Structure

The minimal application structure required:

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── yourcompany/
│   │           └── yourproject/
│   │               ├── Application.java
│   │               ├── workflows/
│   │               │   ├── WorkflowService.java
│   │               │   ├── WorkflowController.java
│   │               │   ├── constants/
│   │               │   │   └── ActivitiKey.java
│   │               │   └── events/
│   │               │       ├── CustomWorkflowActionEvent.java
│   │               │       └── CustomWorkflowEventListener.java
│   │               └── domain/
│   │                   └── YourBusinessModel.java
│   └── resources/
│       ├── application.properties
│       ├── application.yml
│       └── processes/
│           └── your-workflow.bpmn20.xml
```

## Core Components

### 1. WorkflowService

The `WorkflowService` is the central component for interacting with the BPA engine. It handles:

- Starting workflow processes
- Performing actions on tasks
- Checking if tasks exist
- Retrieving task information

Based on the project's `WorkflowService.java`, here's a sample implementation:

```java
@Service
@Log4j2
public class WorkflowService {

    private final BusinessProcessAutomationClient bpaClient;

    public WorkflowService(BusinessProcessAutomationClient bpaClient) {
        this.bpaClient = bpaClient;
    }

    /**
     * Perform a workflow process action (start, approve, reject, etc.)
     */
    public HttpStatus performProcess(WorkflowDto workflowDto) throws Exception {
        TaskPerformRequest request = new TaskPerformRequest();
        request.setModule(ApplicationConstants.ACTIVITI_MODULE_NAME);
        request.setKey(workflowDto.getKey());
        request.setTitle(workflowDto.getTitle());
        request.setRef(workflowDto.getRef());
        request.setAction(new Action(workflowDto.getAction()));
        
        if (workflowDto.getRemarks() != null && !workflowDto.getRemarks().isEmpty()) {
            request.setRemarks(workflowDto.getRemarks());
        }
        
        bpaClient.perform(getAuthenticationToken(), request, workflowDto);
        return HttpStatus.OK;
    }

    /**
     * Check if a task exists for a given key and reference
     */
    public Boolean isTaskExist(String key, String ref) {
        try {
            bpaClient.getActions(getAuthenticationToken(), key, ref);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Start a new workflow process
     */
    @SneakyThrows
    public void initiateBpaWorkflowEvent(String activityKey, String refId, String title, String referenceId) {
        Boolean isTaskExist = this.isTaskExist(activityKey, refId);
        if (!isTaskExist) {
            WorkflowDto workflowDto = new WorkflowDto();
            workflowDto.setKey(activityKey);
            workflowDto.setRef(refId);
            workflowDto.setTitle(title);
            workflowDto.setAction(ApplicationConstants.ACTIVITI_PROCESS_START);
            this.performProcess(workflowDto);
        }
    }

    /**
     * Start a workflow with remarks
     */
    @SneakyThrows
    public void initiateBpaWorkflowEventWithRemarks(String activityKey, String refId, 
                                                   String title, String referenceId, String remarks) {
        Boolean isTaskExist = this.isTaskExist(activityKey, refId);
        if (!isTaskExist) {
            WorkflowDto workflowDto = new WorkflowDto();
            workflowDto.setKey(activityKey);
            workflowDto.setRef(refId);
            workflowDto.setAction(ApplicationConstants.ACTIVITI_PROCESS_START);
            workflowDto.setTitle(title);
            workflowDto.setRemarks(remarks);
            this.performProcess(workflowDto);
        }
    }

    /**
     * Get available actions for a task
     */
    public TaskAction getActions(String key, String ref) {
        return bpaClient.getActions(getAuthenticationToken(), key, ref);
    }
    
    /**
     * Get authentication token for BPA client calls
     * Note: Implement according to your authentication mechanism
     */
    private String getAuthenticationToken() {
        // This is a placeholder. In your application, use your actual 
        // authentication mechanism to get the JWT token
        return "SecurityUtil.getHeaderJwt()";
    }
}
```

### 2. WorkflowController

The REST controller exposes workflow operations to HTTP clients:

```java
@RestController
public class WorkflowRestController {

    private final WorkflowService workflowService;

    public WorkflowRestController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    /**
     * Endpoint to perform workflow actions like approve, reject, etc.
     */
    @PostMapping("/v1/workflow/perform")
    public HttpStatus perform(@RequestBody WorkflowDto workflowDto) throws Exception {
        return workflowService.performProcess(workflowDto);
    }
    
    // Additional endpoints as needed
}
```

### 3. Workflow Event Classes

Two key classes enable event-driven behavior:

#### CustomWorkflowActionEvent

This class extends `WorkflowActionEvent` and represents workflow events in your system:

```java
public class CustomWorkflowActionEvent extends WorkflowActionEvent {
    public CustomWorkflowActionEvent(Object source, WorkflowDto dto, EventType type) {
        super(source, dto, type);
    }
}
```

#### CustomWorkflowEventListener

This listener responds to workflow events:

```java
@Async
@Component
@Log4j2
@RequiredArgsConstructor
public class CustomWorkflowEventListener implements ApplicationListener<CustomWorkflowActionEvent> {

    @Override
    public void onApplicationEvent(CustomWorkflowActionEvent event) {
        if (event.type.equals(EventType.After)) {
            TaskPerformResponse taskPerformResponse = (TaskPerformResponse) event.getSource();
            Action action = taskPerformResponse.getAction();

            if (Objects.isNull(action)) {
                return;
            }

            Long refId = Long.parseLong(event.dto.getRef());

            switch (action.getName().toLowerCase()) {
                case "sent_back":
                    // Handle sent back action
                    break;
                case "sent_for_approval":
                    // Handle sent for approval action
                    break;
                case "rejected":
                    // Handle rejection action
                    break;
                case "approved":
                    // Handle approval action
                    break;
                default:
                    // Handle unknown action
            }
        } 
        else if (event.type.equals(EventType.Before)) {
            // Pre-action validation logic
        } 
        else if (event.type.equals(EventType.Abort)) {
            // Exception handling logic
        }
    }
}
```

### 4. Constants and Enums

Define your workflow process keys and constants:

```java
public enum ActivitiKey {
    PROCESS_A("process_a_key"),
    PROCESS_B("process_b_key");

    private final String description;

    ActivitiKey(String description) {
        this.description = description;
    }
}

public class ApplicationConstants {
    public static final String ACTIVITI_PROCESS_START = "start";
    public static final String ACTIVITI_MODULE_NAME = "your_module_name";
}
```

## Workflow Integration

### Integrating with Business Logic

The following example demonstrates how to integrate workflow with business logic:

```java
@Service
public class BusinessService {
    
    private final WorkflowService workflowService;
    
    public BusinessService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }
    
    public YourBusinessObject processBusinessObject(YourBusinessObject obj) {
        // Business logic
        // ...
        
        // Start workflow process
        workflowService.initiateBpaWorkflowEventWithRemarks(
            ActivitiKey.PROCESS_A.toString(),
            obj.getId().toString(),
            "Process Title for " + obj.getName(),
            "UserId",
            "Initial remarks for process"
        );
        
        return obj;
    }
    
    // Additional business methods
}
```

### Data Exchange with Processes

When starting or performing actions on processes, you can pass data using the WorkflowDto object:

```java
WorkflowDto workflowDto = new WorkflowDto();
workflowDto.setKey(activityKey);  // Process definition key
workflowDto.setRef(refId);        // Business object reference
workflowDto.setTitle(title);      // Process instance title
workflowDto.setAction(action);    // Action to perform (start, approve, etc.)
workflowDto.setRemarks(remarks);  // Optional comments

// You can also add additional variables
Map<String, Object> variables = new HashMap<>();
variables.put("businessObjectId", obj.getId());
variables.put("currentUser", getCurrentUser());
workflowDto.setVariables(variables);
```

## Event Handling System

### Event Flow

1. When a workflow action is performed, the BPA client generates events
2. These events are published with one of three types:
   - `Before`: Triggered before an action is executed
   - `After`: Triggered after an action is completed
   - `Abort`: Triggered when an exception occurs

### Mapping Events to Listeners

In `application.yml`, map process keys to event listener classes:

```yaml
bpaclient:
  events:
    ProcessKeyName: com.yourcompany.yourproject.workflows.events.CustomWorkflowActionEvent
```

### Implementing Business Logic in Event Listeners

Event listeners provide a powerful mechanism for responding to workflow state changes:

```java
@Override
public void onApplicationEvent(CustomWorkflowActionEvent event) {
    if (event.type.equals(EventType.After)) {
        TaskPerformResponse response = (TaskPerformResponse) event.getSource();
        
        // Extract business object ID from the reference
        Long businessObjectId = Long.parseLong(event.dto.getRef());
        
        // Perform different actions based on workflow state
        String actionName = response.getAction().getName().toLowerCase();
        switch (actionName) {
            case "approved":
                // Notify users
                notificationService.notifyApproval(businessObjectId);
                
                // Update business object state
                businessService.markAsApproved(businessObjectId);
                
                // Additional logic
                break;
                
            case "rejected":
                businessService.markAsRejected(businessObjectId);
                notificationService.notifyRejection(businessObjectId);
                break;
                
            // Other cases
        }
    }
}
```

## Configuration Management

### Application Properties

Configure your application properties:

```properties
# Spring Application name
spring.application.name=your-bpa-application

# Server configuration
server.port=8080

# Logging
logging.level.com.yourcompany=DEBUG
```

### Flowable BPA Client Configuration

Configure the BPA client in `application.yml`:

```yaml
bpaclient:
  url: http://your-flowable-bpa-server:8080
  username: admin
  password: test
  events:
    YourProcessKey: com.yourcompany.yourproject.workflows.events.CustomWorkflowActionEvent
```

## API Endpoints

Your application should expose these API endpoints for workflow operations:

| Endpoint | Method | Description | Request Body |
|----------|--------|-------------|-------------|
| `/v1/workflow/perform` | POST | Perform workflow action | `WorkflowDto` |
| `/v1/workflow/tasks/{businessObjectId}` | GET | Get tasks for business object | - |
| `/v1/workflow/actions/{key}/{ref}` | GET | Get available actions | - |
| `/v1/workflow/history/{key}/{ref}` | GET | Get workflow history | - |

### Sample API Request

```json
POST /v1/workflow/perform
{
  "key": "your_process_key",
  "ref": "123",
  "title": "Approval Request for Item #123",
  "action": "approve",
  "remarks": "Approved after review of all documents"
}
```

## Best Practices

### 1. Error Handling

Implement proper error handling in your workflow services:

```java
public HttpStatus performProcess(WorkflowDto workflowDto) {
    try {
        TaskPerformRequest request = createRequestFromDto(workflowDto);
        bpaClient.perform(getAuthenticationToken(), request, workflowDto);
        return HttpStatus.OK;
    } catch (Exception e) {
        log.error("Error performing workflow process: {}", e.getMessage(), e);
        // Handle specific exceptions differently
        if (e instanceof BpaAuthenticationException) {
            return HttpStatus.UNAUTHORIZED;
        } else if (e instanceof BpaNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
```

### 2. Transaction Management

Ensure proper transaction handling, especially when combining database operations with workflow actions:

```java
@Transactional
public void completeBusinessProcess(Long businessObjectId, String action, String remarks) {
    // Database operations
    BusinessObject obj = repository.findById(businessObjectId)
        .orElseThrow(() -> new EntityNotFoundException("Business object not found"));
    
    obj.setStatus(determineStatusFromAction(action));
    repository.save(obj);
    
    // Workflow operations - these should be performed after DB operations
    // to ensure consistency
    WorkflowDto dto = new WorkflowDto();
    dto.setKey(ActivitiKey.YOUR_PROCESS.toString());
    dto.setRef(businessObjectId.toString());
    dto.setAction(action);
    dto.setRemarks(remarks);
    
    workflowService.performProcess(dto);
}
```

### 3. Security Considerations

Always include proper authentication when making BPA client calls:

```java
private String getAuthenticationToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    // Extract and return JWT token
}
```

### 4. Testing Workflow Integration

Create tests for your workflow integration:

```java
@SpringBootTest
class WorkflowServiceTest {

    @Autowired
    private WorkflowService workflowService;
    
    @MockBean
    private BusinessProcessAutomationClient bpaClient;
    
    @Test
    void testInitiateWorkflow() {
        // Arrange
        String key = ActivitiKey.PROCESS_A.toString();
        String ref = "123";
        when(bpaClient.getActions(anyString(), eq(key), eq(ref)))
            .thenThrow(new RuntimeException("Task not found"));
        
        // Act
        workflowService.initiateBpaWorkflowEvent(key, ref, "Test Title", "User123");
        
        // Assert
        verify(bpaClient).perform(anyString(), any(TaskPerformRequest.class), any(WorkflowDto.class));
    }
}
```

## Troubleshooting

### Common Issues and Solutions

1. **Authentication Failures**
   - Check that your authentication token is correctly generated and passed to the BPA client
   - Verify that the user has appropriate permissions in the Flowable engine

2. **Process Not Found**
   - Ensure the process key matches the deployed process definition
   - Check if the process has been deployed to the Flowable engine

3. **Task Action Failures**
   - Verify that the action is valid for the current state of the task
   - Check if the user has permission to perform the action

4. **Event Listener Not Triggered**
   - Ensure the mapping in `application.yml` is correct
   - Check that the event listener bean is properly registered as a Spring component

### Logging and Debugging

Enable detailed logging to help diagnose issues:

```properties
# application.properties
logging.level.com.bracits.abs.bpaclient=DEBUG
logging.level.com.yourcompany.workflows=DEBUG
```

## Conclusion

This documentation provides a comprehensive guide to integrating Flowable BPA with Spring Boot applications. By following these patterns and best practices, you can create robust, workflow-driven applications that leverage the power of business process automation.

The examples and code snippets are based on the `bpa-backend-starter` project structure, adapted for general use. For specific implementation details, always refer to the latest Flowable documentation and the specific requirements of your project.