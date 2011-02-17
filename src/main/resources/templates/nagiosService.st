define service{
	use                         generic-service
	host_name                   $hostName$
	service_description         $serviceName$
	active_checks_enabled       0
	passive_checks_enabled      1
	check_command               check_http
}