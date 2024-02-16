rootProject.name = "balancer"

include("model")
include("utils")
include("worker_controller")
include("master_controller")
include("load_balancer")
include("test_image")
include("performance_tester")
include("balancer-ui")
include("scaller")
include("scalling_controller")
include("rabbitmq")
include("scalling_controller:scalling_api")
findProject(":scalling_controller:scalling_api")?.name = "scalling_api"
