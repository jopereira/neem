ips="lhona lhufa ucha inha"

for ip in $ips;
  do
   ssh $ip "killall java"
  done

# arch-tag: 6a06f885-d3c4-4432-ac17-3fc0f228d94f
