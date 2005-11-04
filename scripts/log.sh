


for ((p = 1 ; p <= 20; p++ ))
  do
    pr=`expr 12345 + $p`
    grep "recv: inha/192.168.82.187:$pr" exec.inha\:$pr.log > logs/recv.inha.$pr.log
  done


# arch-tag: 1fe6916b-3523-4e84-90ea-4054f6f14289
