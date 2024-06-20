@Autowired
private RedisService redisService;

public void testExistsMultipleKeys() {
List<String> keys = Arrays.asList("key1", "key2", "key3");
boolean exists = redisService.existsMultipleKeys(keys);
System.out.println("All keys exist: " + exists);
}