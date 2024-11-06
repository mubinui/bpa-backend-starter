package com.bracits.bpabackendstarter;

import org.springframework.stereotype.Service;

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
    workflowService.initiateBpaWorkflowEventWithRemarks(ActivitiKey.AcitvitiKeyName1.toString(),
        myClass.featureId + "",
        "Your Title",
        "ExampleusernameId",
        "Remarks");
    return myClass;
  }
}
