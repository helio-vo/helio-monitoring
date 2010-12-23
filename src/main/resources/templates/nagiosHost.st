define host{
	use                         generic-host        ; Name of host template to use
	host_name                   $hostName$
	alias                       $hostName$
	address                     127.0.0.1           ; not important, Nagios gets all data from the monitoring service
}