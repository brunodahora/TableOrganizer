package table.organizer.model;

import java.util.ArrayList;
import java.util.List;

import table.organizer.exceptions.DuplicatePersonException;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TableManager {
	private int nextId;
	private List<Person> persons;
	private List<Consumable> consumables;
	private int tip;
	
	public final String POSITION = "POSITION";
	
	private static TableManager tableManager = new TableManager();

	public static TableManager getInstance(){
		return tableManager;
	}
	
	public String printPrice (int cents) {
		String cent;
		if (cents%100 < 10)
			cent = "0" + cents%100;
		else
			cent = "" + cents%100;
		String price = "$" + cents/100 + "." + cent;
		return price;
	}
	
	private TableManager() {
		persons = new ArrayList<Person>();
		consumables = new ArrayList<Consumable>();
		
		nextId = 0;
		tip = 10;
	}
	
	public int getTip() {
		return tip;
	}

	/**
	 * 
	 * @param tip in percentage
	 */
	public void setTip(int tip) {
		if(tip<0){
			tip=0;
		}
		
		this.tip = tip;
	}

	/**
	 * 
	 * @return total bill price in cents
	 */
	public int getTotalBill(){
		
		int price = 0;
		
		for(Consumable consumable : consumables){
			price += consumable.getTotalPrice();
		}
		
		return price;
	}
	
	public Person addPerson(String name) throws DuplicatePersonException {

		for (Person person : persons) {
			if(person.getName().equals(name)){
				throw new DuplicatePersonException("Person already exists");
			}
		}
		
		Person newPerson = new Person(name);
		
		persons.add(newPerson);
		
		return newPerson;
	}
	
	public boolean removePerson(String name){
		Person person = getPersonByName(name);
		if(person == null){
			return false;
		}
		for (Consumable consumable : person.getConsumables()) {
			consumable.removePerson(person);
		}
		return persons.remove(new Person(name));
	}
	
	private Person getPersonByName(String name) {
		for (Person person : persons) {
			if(person.getName().equals(name)){
				return person;
			}
		}
		return null;
	}

	public Consumable addConsumable(String name, int price, int quantity){
		
		Consumable newConsumable = new Consumable(name, price, quantity, ++nextId);
		
		consumables.add(newConsumable);
		
		return newConsumable;
	}
	
	public boolean removeConsumable(int id){
		Consumable consumable = getConsumableById(id);
		if (consumable == null)
			return false;
		for (Person person : consumable.getPersons()) {
			person.removeConsumable(consumable);
		}
		return consumables.remove(consumable);
	}
	
	private Consumable getConsumableById(int id) {
		for (Consumable consumable : consumables) {
			if (consumable.getId() == id){
				return consumable;
			}
		}
		return null;
	}

	public void addConsumableToPerson(Consumable consumable, Person person){
		consumable.addPerson(person);
		person.addConsumable(consumable);
	}
	
	public void removeConsumableFromPerson(Consumable consumable, Person person) {
		consumable.removePerson(person);
		person.removeConsumable(consumable);
	}

	public int getNumberOfConsumables () {
		return consumables.size();
	}
	
	public Consumable getConsumable (int position) {
		return consumables.get(position);
	}
	
	public List<Person> getPersons() {
		return persons;
	}

	public List<Consumable> getConsumables() {
		return consumables;
	}

	public Person getPerson (int position) {
		return persons.get(position);
	}

	public int getNumberOfPersons() {
		return persons.size();
	}
	
	private final String PERSON_TABLE = "Person";
	private final String CONSUMABLE_TABLE = "Consumable";
	private final String CONSUMES_TABLE = "Consumes";
	
	private static final String DATABASE_NAME = "tableorganizer";
	private static final String DATABASE_CREATE_PERSON = "create table Person(name text PRIMARY KEY NOT NULL UNIQUE);";
	private static final String DATABASE_CREATE_CONSUMABLE = "create table Consumable(id integer PRIMARY KEY, name text NOT NULL, price integer NOT NULL, quantity integer NOT NULL);";
	private static final String DATABASE_CREATE_CONSUMES = "create table Consumes(person text, consumable integer, FOREIGN KEY(person) REFERENCES Person(name), FOREIGN KEY(consumable) REFERENCES Consumable(id), UNIQUE(person, consumable)); ";

	private static final int DATABASE_VERSION = 3;
	
	private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	private Context context;
	private static TableManager instance;
    
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        	Log.d("DB", "Criando bancos");
            db.execSQL(DATABASE_CREATE_PERSON);
            db.execSQL(DATABASE_CREATE_CONSUMABLE);
            db.execSQL(DATABASE_CREATE_CONSUMES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
//                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS Person");
            onCreate(db);
        }
    }
    
    private TableManager(Context ctx){
    	context = ctx;
    }
    
    public TableManager open() throws SQLException {
    	mDbHelper = new DatabaseHelper(context);
    	mDb = mDbHelper.getWritableDatabase();
    	return this;
    }

    public long createPerson(String name)
    {
    	ContentValues values = new ContentValues();
    	values.put("name", name);
    
    	return mDb.insert(PERSON_TABLE, null, values);
    }
    
    public List<Person> fetchPersons()
    {
    	List<Person> persons = new ArrayList<Person>();
    	
    	//Cursor c = mDb.query("Person", new String [] {"name"}, "name=?",
    	//		new String[] {name}, null, null, null);
    	Cursor c = mDb.query(PERSON_TABLE, new String [] {"name"}, 
    			null, null, null, null, null);

    	c.moveToFirst();
    	int size = c.getCount(); 
    	for(int i = 0; i < size; i++, c.moveToNext()){
    		String fetchedName = c.getString(c.getColumnIndex("name"));
    		persons.add(new Person(fetchedName));
    	}
    	
    	return persons;
    }
    
    public void deletePerson(String name){
    	//TODO
    }
    
    public long createConsumable(String name, Integer price, Integer quantity){
    	ContentValues values = new ContentValues();
    	values.put("id", "null");
    	values.put("name", name);
    	values.put("price", price);
    	values.put("quantity", quantity);
    	
    	return mDb.insert(CONSUMABLE_TABLE, null, values);
    }
        
    public void deleteConsumable(int id) {
    	// TODO Auto-generated method stub
    }
    
    public List<Consumable> fetchConsumables(){
    	List <Consumable> consumables = new ArrayList<Consumable>();
    	
    	Cursor c = mDb.query(CONSUMABLE_TABLE, new String[] {"id", "name", "price", "quantity"}, 
    			null, null, null, null, null);
    	c.moveToFirst();
    	int size = c.getCount();
    	for (int i = 0; i < size; i++, c.moveToNext()){
    		int id, price, quantity;
    		String name;
    		id = c.getInt(c.getColumnIndex("id"));
    		name = c.getString(c.getColumnIndex("name"));
    		price = c.getInt(c.getColumnIndex("price"));
    		quantity = c.getInt(c.getColumnIndex("quantity"));

    		consumables.add(new Consumable(name, price, quantity, id));
    	}
    	
    	return consumables;
    }

	public synchronized static TableManager getInstance(Context context) {
		if (instance == null) {
			instance = new TableManager(context);
		}
		return instance;
	}


}