define host{
	use                   	  	generic-host            	; Name of host template to use
	host_name             	  	msslxw.mssl.ucl.ac.uk
	alias                 	  	msslxw.mssl.ucl.ac.uk
	check_command				check_http!-p 8080
	address                		128.40.70.112
	}

;DPAS
define service{
	use							generic-service
	host_name					msslxw.mssl.ucl.ac.uk
	service_description			DPAS -ping-
	active_checks_enabled   	0
	passive_checks_enabled  	1
	check_command				check_http
	}

define service{
	use							generic-service
	host_name					msslxw.mssl.ucl.ac.uk
	service_description			DPAS -method call-
	active_checks_enabled   	0
	passive_checks_enabled  	1
	check_command				check_http
	}
	
define service{
	use							generic-service
	host_name					msslxw.mssl.ucl.ac.uk
	service_description			DPAS -testing-
	active_checks_enabled   	0
	passive_checks_enabled  	1
	check_command				check_http
	}
	
;ICS_R2
define service{
	use							generic-service
	host_name					msslxw.mssl.ucl.ac.uk
	service_description			ICS_R2 -ping-
	active_checks_enabled   	0
	passive_checks_enabled  	1
	check_command				check_http
	}

define service{
	use							generic-service
	host_name					msslxw.mssl.ucl.ac.uk
	service_description			ICS_R2 -method call-
	active_checks_enabled   	0
	passive_checks_enabled  	1
	check_command				check_http
	}
	
define service{
	use							generic-service
	host_name					msslxw.mssl.ucl.ac.uk
	service_description			ICS_R2 -testing-
	active_checks_enabled   	0
	passive_checks_enabled  	1
	check_command				check_http
	}