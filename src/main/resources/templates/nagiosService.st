define service{
	use                         generic-service
	host_name                   $hostName$
	service_description         $serviceName$ -ping-
	active_checks_enabled       0
	passive_checks_enabled      1
	check_command               check_http
}
define service{
	use                         generic-service
	host_name                   $hostName$
	service_description         $serviceName$ -method call-
	active_checks_enabled       0
	passive_checks_enabled      1
	check_command               check_http
}
define service{
	use                         generic-service
	host_name                   $hostName$
	service_description         $serviceName$ -testing-
	active_checks_enabled       0
	passive_checks_enabled      1
	check_command               check_http
}