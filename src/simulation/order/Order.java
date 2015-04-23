package simulation.order;

public class Order implements IMessage{
	
	private static final long serialVersionUID = -6715728675040627672L;		//generated serialVersionUID
	
	private String agentId;
    private Double price;
    private Integer volume;
    private Integer direction;  //0 mean sell, 1 mean buy.
    private Integer orderId;
    private Long createTime;
    
    public static final int SELL_ORDER = 0;
    public static final int BUY_ORDER = 1;
    
    public Order() {
    	this.orderId = 0;
        this.agentId = "";
        this.price = 0d;
        this.volume = 0;
        this.direction = 0;
    }

    public Order(Integer id, String agentId, Double price, Integer volume, Integer direction, Long createTime) {
        this.orderId = id;
    	this.agentId = agentId;
        this.price = price;
        this.volume = volume;
        this.direction = direction;
        this.createTime = createTime;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getPrice() {
        return price;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setDirection(Integer direction) {
        this.direction = direction;
    }

    public Integer getDirection() {
        return direction;
    }

    public Long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

	public Integer getOrderId() {
		return orderId;
	}
	
	public void setOrderId(Integer orderId) {
		this.orderId = orderId;
	}

	@Override
	public String convertSelfToString() {
		Integer lackLength;
		
		String idStr = String.valueOf( orderId );
		if(idStr.length() <= 6){
            lackLength = 6 - idStr.length();
            for(int i = 0; i < lackLength; i++){
                idStr = "0" + idStr;
            }
        }
        else{
        	idStr = idStr.substring(idStr.length() - 6);
        }
		
        String agentIdStr = String.valueOf(agentId);
        if(agentIdStr.length() <= 8){
            lackLength = 8 - agentIdStr.length();
            for(int i = 0; i < lackLength; i++){
                agentIdStr = "0" + agentIdStr;
            }
        }
        else{
            agentIdStr = agentIdStr.substring(agentIdStr.length() - 8);
        }
        
        String volumeStr = String.valueOf(volume);
        if(volumeStr.length() <= 10){
            lackLength = 10 - volumeStr.length();
            for(int i = 0; i < lackLength; i++){
                volumeStr = "0" + volumeStr;
            }
        }
        else{
            volumeStr = volumeStr.substring(0,10);
        }
        
        String priceStr = String.valueOf(price);
        if(priceStr.length() <= 8){
            lackLength = 8 - priceStr.length();
            for(int i = 0; i < lackLength; i++){
                priceStr = "0" + priceStr;
            }
        }
        else{
            priceStr = priceStr.substring( 0,8 );
        }
        
        
        String directionStr = String.valueOf(direction);

//        String timeStr = Long.toString(createTime);
        
        return idStr + agentIdStr + priceStr + volumeStr + directionStr /*+ timeStr*/;
	}
	
	@Override
	public String toString() {
		return convertSelfToString();
	}
	
	@Override
	public void setStringForSelf(String message) {
		
		String idStr = message.substring(0, 6);
		this.setOrderId(Integer.parseInt(idStr));
        
        String agentIdStr = message.substring(6, 14);
        this.setAgentId( agentIdStr );
        
        String priceStr = message.substring(14, 22);
        this.setPrice(Double.parseDouble(priceStr));
        
        String volumeStr = message.substring(22, 32);
        this.setVolume(Integer.parseInt(volumeStr));
      
        String directionStr = message.substring(32, 33);
        this.setDirection(Integer.parseInt(directionStr));
        
        String timeStr = message.substring(33);
        if( null!= timeStr && !"".equals(timeStr.trim()) )
        	this.setCreateTime(Long.parseLong(timeStr));
        else
        	this.setCreateTime(System.currentTimeMillis());
//        or.setCreateTime(Long.parseLong(timeStr));
        
	}
}
