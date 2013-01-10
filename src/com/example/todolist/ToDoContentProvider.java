package com.example.todolist;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

// - Provides an interface for publishing and consuming data,
//   based around a simple URI addressing model using the
//   content:// schema.
// - Defined in manifest:
//   Authorities tag define the base URI of the Provider’s authority.
//   Provider’s authority is used by the Content Resolver as an address
//   and used to find the database that we want to interact with.
public class ToDoContentProvider extends ContentProvider { // Abstracts the underlying data layer
    // Data path to the primary content.
    public static final Uri CONTENT_URI =
            Uri.parse("content://com.example.todoprovider/todoitems");

    private static final int ALLROWS = 1;
    private static final int SINGLE_ROW = 2;
    private static final UriMatcher uriMatcher; // Parses URIs and determines their form.

    // URI ending in 'todoitems' corresponds to a request for all items
    // URI ending in 'todoitems/[rowID]' corresponds to a single row.
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.example.todoprovider", "todoitems", ALLROWS);
        uriMatcher.addURI("com.example.todoprovider", "todoitems/#", SINGLE_ROW);
    }

    // Practice is to create a public field for each column in the table.
    public static final String KEY_ID = "_id";
    public static final String KEY_TASK = "task";

    private ToDoDBSQLiteOpenHelper mDBOpenHelper;

    @Override
    public boolean onCreate() {
        // Initialize the underlying data source.
        mDBOpenHelper = new ToDoDBSQLiteOpenHelper(getContext(),
                ToDoDBSQLiteOpenHelper.DATABASE_NAME, null,
                ToDoDBSQLiteOpenHelper.DATABASE_VERSION);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, // projection is columns to include in result set
                        String[] selectionArgs, String sortOrder) {
        // Get readable instance to db (behind the scenes, creates db if necessary).
        final SQLiteDatabase db = mDBOpenHelper.getReadableDatabase();

        // SQLiteQueryBuilder is a helper to build row-based queries
        final SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        // Specify the table on which to perform the query.
        queryBuilder.setTables(ToDoDBSQLiteOpenHelper.TABLE_NAME);

        // If this is a row query, limit the result set to the passed in row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW: {
                String rowID = uri.getPathSegments().get(1);
                queryBuilder.appendWhere(KEY_ID + "=" + rowID);
                break;
            }
            default: {
                break;
            }
        }

        // Execute the query.
        final Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);

        // Return the result cursor.
        return cursor;
    }

    @Override
    public String getType(Uri uri) { // Must implemented to identify data retured by query().
        // Return a string that identifies the MIME type for a Content Provider URI.
        switch (uriMatcher.match(uri)) {
            case ALLROWS: {
                return "vnd.android.cursor.dir/vnd.example.todos";
            }
            case SINGLE_ROW: {
                return "vnd.android.cursor.item/vnd.example.todos";
            }
            default: {
                throw new IllegalArgumentException("Unsupported URI: " + uri);
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mDBOpenHelper.getWritableDatabase(); // Get writeable instance to db

        // Insert the row into the table
        final long id = db.insert(ToDoDBSQLiteOpenHelper.TABLE_NAME, null, values);

        if (id > -1) {
            // Construct and return the URI of the newly inserted row.
            // ContentUris.withAppendedId appends the ID to the CONTENT_URI.
            final Uri insertedId = ContentUris.withAppendedId(CONTENT_URI, id);

            // Notify any observers of the change in the data set.
            getContext().getContentResolver().notifyChange(insertedId, null);

            return insertedId;
        } else {
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        final SQLiteDatabase db = mDBOpenHelper.getWritableDatabase(); // Get writeable instance to db

        // If this is a row URI, limit the deletion to the specified row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW: {
                String rowID = uri.getPathSegments().get(1);
                selection = KEY_ID + "=" + rowID
                        + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : "");
                break;
            }
            default: {
                break;
            }
        }

        // Perform the update on row(s) matching the selection.
        final int updateCount = db.update(ToDoDBSQLiteOpenHelper.TABLE_NAME,
                values, selection, selectionArgs);

        // Notify  observers of the change in the data set.
        getContext().getContentResolver().notifyChange(uri, null);

        return updateCount;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mDBOpenHelper.getWritableDatabase(); // Get writeable instance to db

        // If this is a row URI, limit the deletion to the specified row.
        switch (uriMatcher.match(uri)) {
            case SINGLE_ROW: {
                final String rowID = uri.getPathSegments().get(1);
                selection = KEY_ID + "=" + rowID
                        + (!TextUtils.isEmpty(selection) ?
                        " AND (" + selection + ')' : "");
                break;
            }
            default: {
                break;
            }
        }

        // To return the number of deleted items, you must specify a where
        // clause. To delete all rows and return a value, pass in "1".
        if (selection == null) {
            selection = "1";
        }

        // Delete the row(s) matching the selection.
        int deleteCount = db.delete(ToDoDBSQLiteOpenHelper.TABLE_NAME, selection, selectionArgs);

        // Notify any observers of the change in the data set.
        getContext().getContentResolver().notifyChange(uri, null);

        return deleteCount;
    }

    // SQLite is implemented as a library, rather than running as a separate process.
    // Each SQLite database is an integrated part of the application that created it.
    // SQLite is extremely reliable.
    // SQLiteOpenHelper exposes query, insert, update, delete operations.
    private static class ToDoDBSQLiteOpenHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "todo.db"; // Will be stored in /data/data/<package>/databases
        private static final int DATABASE_VERSION = 1; // Important for upgrade paths
        private static final String TABLE_NAME = "todoItemTable";

        public ToDoDBSQLiteOpenHelper(Context context, String name,
                                      CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        // SQL statement to create a new database.
        private static final String CREATE_TABLE = "create table " + TABLE_NAME
                + " (" + KEY_ID + " integer primary key autoincrement, "
                + KEY_TASK + " text not null);";

        // Called when no database exists in disk.
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE);
        }

        // Called when the app database version mismatches the disk database version.
        // Used to upgrade db the current version.
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // The simplest case is to drop the old table and create a new one.
            db.execSQL("DROP TABLE IF IT EXISTS " + TABLE_NAME); // Migrate existing data if necessary before this.
            // Create a new one.
            onCreate(db);
        }
    }
}
