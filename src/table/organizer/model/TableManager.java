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
	private List<Person> persons;
	private List<Consumable> consumables;
	
	public final String POSITION = "POSITION";
	
	private final String PERSON_TABLE = "Person";
	private final String CONSUMABLE_TABLE = "Consumable";
	private final String CONSUMES_TABLE = "Consumes";
	
	private static final String DATABASE_NAME = "tableorganizer";
	private static final String DATABASE_CREATE_PERSON = "create table Person(name text PRIMARY KEY NOT NULL UNIQUE);";
	private static final String DATABASE_CREATE_CONSUMABLE = "create table Consumable(id integer PRIMARY KEY, name text NOT NULL, price integer NOT NULL, quantity integer NOT NULL);";
	private static final String DATABASE_CREATE_CONSUMES = "create table Consumes(person text, consumable integer, FOREIGN KEY(person) REFERENCES Person(name), FOREIGN KEY(consumable) REFERENCES Consumable(id), UNIQUE(person, consumable)); ";

	private static final int DATABASE_VERSION = 3;
	public static final int DEFAULT_TIP = 0;
	
	private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
	private Context context;
	private static TableManager instance;
	private int tip;
	
    private TableManager(Context ctx){		

    	context = ctx;
    	
		open();
		
		persons = fetchPersons();
		consumables = fetchConsumables();
		fetchRelations();
		tip = DEFAULT_TIP;
    }
    
	public TableManager open() throws SQLException {
    	mDbHelper = new DatabaseHelper(context);
    	mDb = mDbHelper.getWritableDatabase();
    	return this;
    }
    
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
    
	public synchronized static TableManager getInstance(Context context) {
		if (instance == null) {
			instance = new TableManager(context);
		}
		return instance;
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
	
	public int getTotalBillWithTip(){
		return (int)(getTotalBill()*(100+tip))/100;
	}
	
	public Person addPerson(String name) throws DuplicatePersonException {

		if(createPerson(name) == -1)
			throw new DuplicatePersonException("Person already exists");
		
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
		
		mDb.delete(CONSUMES_TABLE, "person=?", new String[] {name});
		deletePerson(name);

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

	public Consumable addConsumable(String name, int price, int quantity) throws Exception {
		int id = (int) createConsumable(name, price, quantity);
		
		Consumable newConsumable = new Consumable(name, price, quantity, id);
		
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
		
		mDb.delete(CONSUMES_TABLE, "consumable=?", new String[] {id+""});
		deleteConsumable(id);
		
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
		if(consumable != null && person != null){
			consumable.addPerson(person);
			person.addConsumable(consumable);
			
			ContentValues values = new ContentValues();
			values.put("person", person.getName());
	    	values.put("consumable", consumable.getId());
	    	
	    	mDb.insert(CONSUMES_TABLE, null, values);
		}
	}
	
	public void removeConsumableFromPerson(Consumable consumable, Person person) {
		consumable.removePerson(person);
		person.removeConsumable(consumable);
		
		mDb.delete(CONSUMES_TABLE, "person=? AND consumable=?", new String[] {person.getName(), consumable.getId()+""});
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
    
    //BD Methods	
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
    	mDb.delete(PERSON_TABLE, "name=?", new String[] {name});
    }
    
    public long createConsumable(String name, Integer price, Integer quantity) throws Exception {
    	ContentValues values = new ContentValues();
    	values.put("name", name);
    	values.put("price", price);
    	values.put("quantity", quantity);
    	
    	long id = mDb.insert(CONSUMABLE_TABLE, null, values);
    	
    	if(id == -1)
    		throw new Exception("Não foi possível inserir consumable");
    	
    	return id;
    }
        
    public void deleteConsumable(Integer id) {
    	mDb.delete(CONSUMABLE_TABLE, "id=?", new String[] {id.toString()});
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

    private void fetchRelations() {
    	
    	Cursor c = mDb.query(CONSUMES_TABLE, new String [] {"person", "consumable"}, 
    			null, null, null, null, null);

    	c.moveToFirst();
    	int size = c.getCount(); 
    	for(int i = 0; i < size; i++, c.moveToNext()){
    		Person relPerson = null;
    		Consumable relConsumable = null;
    		
    		String fetchedPerson = c.getString(c.getColumnIndex("person"));
    		int fetchedConsumable = c.getInt(c.getColumnIndex("consumable"));
    		
    		for (Person person : persons) {
				if(person.getName().equals(fetchedPerson)){
					relPerson = person;
					break;
				}
			}
    		for (Consumable consumable : consumables) {
				if(consumable.getId() == fetchedConsumable){
					relConsumable = consumable;
					break;
				}
			}

    		addConsumableToPerson(relConsumable, relPerson);
    	}
    	
	}

	public void clear() {
		persons.clear();
		consumables.clear();
		mDb.delete(CONSUMES_TABLE, null, null);
		mDb.delete(PERSON_TABLE, null, null);
		mDb.delete(CONSUMABLE_TABLE, null, null);
	}
	
	public int getTip() {
		return tip;
	}

	public void setTip(int tip) {
		this.tip = tip;
	}

	public int getPersonalBill(Person person) {
		return (person.getPersonalBill()*(100+tip))/100;
	}

}
