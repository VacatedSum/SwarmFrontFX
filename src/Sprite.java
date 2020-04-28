
public class Sprite {
	private int x;
	private int y;
	public final int type; //1 for zombie, 2 for player
	private String id;
	public Sprite(String id, int x, int y) {
		if (id.charAt(0) == 'p') {
			type = 2;
		} else {
			type = 1;
		}
		this.setId(id);
		this.setX(x);
		this.setY(y);
		
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
}
