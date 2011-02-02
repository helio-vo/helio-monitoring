define host{
	use                         generic-host        ; Name of host template to use
	host_name                   $hostName$
	alias                       $hostName$
	address                     $hostAddress$       ; not important, Nagios gets all data from the monitoring service
}