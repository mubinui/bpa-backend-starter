# Flowable Business Process Automation Service Backend Template

This project serves as a template for implementing a backend service that integrates with Flowable Business Process Automation (BPA). It provides a foundation for building workflow-driven applications using Spring Boot and Flowable.

## Table of Contents

1. [Overview](#overview)
2. [Key Components](#key-components)
3. [Workflow Integration](#workflow-integration)
4. [API Endpoints](#api-endpoints)
5. [Event Handling](#event-handling)
6. [Configuration](#configuration)
7. [Getting Started](#getting-started)
8. [Usage Example](#usage-example)

## Overview

This template provides a structure for creating a backend service that leverages Flowable BPA for workflow management. It includes components for handling workflow actions, processing events, and integrating with business logic.

## Key Components

### WorkflowRestController

The `WorkflowRestController` exposes REST endpoints for performing workflow actions. It uses the `WorkflowService` to handle the business logic.

### WorkflowService

The `WorkflowService` is responsible for interacting with the Flowable BPA client. It provides methods for:

- Performing workflow processes
- Checking if a task exists
- Initiating BPA workflow events
- Retrieving workflow actions

### MyClassWorkflowEventListener

This component listens for workflow action events and handles them based on the event type (Before, After, Abort) and the specific action performed.

### MyClassService

An example service that demonstrates how to integrate business logic with workflow processes.

## Workflow Integration

The template uses the `BusinessProcessAutomationClient` to interact with Flowable BPA. Workflow processes are initiated and managed through this client.

## API Endpoints

- `POST /v1/workflow/perform`: Performs a workflow action based on the provided `WorkflowDto`.

## Event Handling

The `MyClassWorkflowEventListener` handles workflow events:

- **Before**: Used for validation before an action is performed.
- **After**: Handles post-action logic based on the specific action (e.g., sent back, sent for approval, rejected, approved).
- **Abort**: Handles scenarios where the workflow encounters an exception.

## Configuration

The `application.yml` file contains configuration for BPA client events:

```yaml
bpaclient:
  events:
    NameOfTheKey: com.bracits.example.event.MyClassWorkflowEventListener

```
## Usage Example

Here's a basic example of how to use this template:

1. Define your workflow keys in the `ActivitiKey` enum:
```java
public enum ActivitiKey {
    AcitvitiKeyName1("key1"),
    AcitvitiKeyName2("key2");
    
    private final String description;
    
    ActivitiKey(String description) {
        this.description = description;
    }
}
```
2.  Implement Event Handling in MyClassWorkflowEventListener

To respond to workflow actions, implement event handling in `MyClassWorkflowEventListener`:

```java
@Override
public void onApplicationEvent(MyClassWorkflowActionEvent event) {
    if (event.type.equals(EventType.After)) {
        TaskPerformResponse response = (TaskPerformResponse) event.getSource();
        Action action = response.getAction();
        
        switch (action.getName().toLowerCase()) {
            case "approved":
                // Handle approval logic
                break;
            case "rejected":
                // Handle rejection logic
                break;
            case "sent_back":
                // Handle sent back logic
                break;
            case "sent_for_approval":
                // Handle sent for approval logic
                break;
            default:
                // Handle default case
        }
    } else if (event.type.equals(EventType.Before)) {
        // Add validation logic before workflow action
    } else if (event.type.equals(EventType.Abort)) {
        // Handle workflow exceptions
    }
}
```
This snippet demonstrates how to use the `WorkflowService` to initiate a workflow and how to handle workflow events in the `MyClassWorkflowEventListener`.
3. Implementing Business Logic and REST Endpoints

Create a service to implement your business logic:

```java
@Service
public class MyClassService {
    private final WorkflowService workflowService;

    public MyClassService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public MyClass getMyClass() {
        MyClass myClass = new MyClass();
        myClass.setFeatureId(1L);
        myClass.setFeatureName("Feature 1");
        myClass.setFeatureDescription("Feature 1 Description");
        
        // Initiate workflow
        workflowService.initiateBpaWorkflowEventWithRemarks(
            ActivitiKey.AcitvitiKeyName1.toString(),
            myClass.getFeatureId().toString(),
            "Your Title",
            "ExampleusernameId",
            "Remarks"
        );
        
        return myClass;
    }
}
```

Use the `WorkflowRestController` to expose endpoints for performing workflow actions:

```java
@PostMapping("/v1/workflow/perform")
public HttpStatus perform(@RequestBody WorkflowDto workflowDto) {
    return workflowService.performProcess(workflowDto);
}
```

This example demonstrates how to initiate a workflow, handle workflow events, implement business logic, and expose REST endpoints for workflow actions.