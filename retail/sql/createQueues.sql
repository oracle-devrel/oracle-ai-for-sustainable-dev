begin
   dbms_aqadm.create_sharded_queue(
      queue_name         => 'orderqueue',
      queue_payload_type => dbms_aqadm.jms_type,
      multiple_consumers => true
   );

   dbms_aqadm.create_sharded_queue(
      queue_name         => 'inventoryqueue',
      queue_payload_type => dbms_aqadm.jms_type,
      multiple_consumers => true
   );

   dbms_aqadm.start_queue(queue_name => 'orderqueue');
   dbms_aqadm.start_queue(queue_name => 'inventoryqueue');
end;
/



begin
   dbms_aqadm.grant_queue_privilege(
      privilege    => 'ENQUEUE',
      queue_name   => 'admin.orderqueue',
      grantee      => 'orderuser',
      grant_option => false
   );

   dbms_aqadm.grant_queue_privilege(
      privilege    => 'DEQUEUE',
      queue_name   => 'admin.orderqueue',
      grantee      => 'inventoryuser',
      grant_option => false
   );

   dbms_aqadm.grant_queue_privilege(
      privilege    => 'ENQUEUE',
      queue_name   => 'admin.inventoryqueue',
      grantee      => 'inventoryuser',
      grant_option => false
   );

   dbms_aqadm.grant_queue_privilege(
      privilege    => 'DEQUEUE',
      queue_name   => 'admin.inventoryqueue',
      grantee      => 'orderuser',
      grant_option => false
   );

end;
/

begin
   dbms_aqadm.add_subscriber(
      queue_name => 'ADMIN.ORDERQUEUE',
      subscriber => sys.aq$_agent(
         'INVENTORY_SERVICE',
         null,
         null
      )
   );

   dbms_aqadm.add_subscriber(
      queue_name => 'ADMIN.INVENTORYQUEUE',
      subscriber => sys.aq$_agent(
         'ORDER_SERVICE',
         null,
         null
      )
   );
end;
/