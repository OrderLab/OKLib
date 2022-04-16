## Project Structure

As required by "Completeness" requirement in Artifact Evaluation, here we highlight the relations of modules and functionalities described in the paper.

* `src` contains main java codes to implement tool functionality
  * `src/main/java/oathkeeper/runtime/DynamicClassModifier.java` is the dynamic instrumentation module, which is described in the paper `Section 8.2 Instrumentation and Trace Generation`
  * `src/main/java/oathkeeper/engine/tester/TestEngine.java` is the test scheduler to schedule test execution to generate traces, which is described in the paper `Section 8.2 Instrumentation and Trace Generation`
  * `src/main/java/oathkeeper/engine/InferEngine.java` is the rule inference module, which is described in the paper `Section 8.3 Template-Driven Inference`
  * `src/main/java/oathkeeper/engine/tester/TestEngine.java` also serves as entry for rule validation. Checking logic is in `src/main/java/oathkeeper/runtime/RuntimeChecker.java`, which is described in the paper `Section 8.4 Rule Validation`
  * `src/main/java/oathkeeper/runtime/RuntimeChecker.java` also handles production detection, as described in `Section 8.5 Runtime Checking`
  * The technical details described in `Section 8.6 Optimization` are inlined with existing module implementation and do not have a seperate module. 

* `conf` contains pre-set sample configurations

* `experiments` contains scripts to reproduce the experiments

* `misc` contains helper scripts

