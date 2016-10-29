package com.zhi.service;

import java.io.File;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 用来和数据库打交道
 */

public class UploadLogService {
	private DBOpenHelper dbOpenHelper;
	
	public UploadLogService(Context context){
		dbOpenHelper = new DBOpenHelper(context);
	}

	/**
	 * 获取 sourceId 的值
	 * @param file  要上传的文件
	 * @return
	 */
	public String getBindId(File file){
		SQLiteDatabase db = dbOpenHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("select sourceId from uploadLog where path=?", new String[]{file.getAbsolutePath()});
		if(cursor.moveToFirst()){
			return cursor.getString(0);
		}
		return null;
	}
	/**
	 * 往数据库中保存 要上传的文件 File及 sourceId
	 * @param sourceId
	 * @param file
	 */
	public void save(String sourceId, File file){
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		db.execSQL("insert into uploadLog(path,sourceId) values(?,?)",
				new Object[]{file.getAbsolutePath(), sourceId});
	}
	/**
	 * 删除数据库中的文件信息（在上传完毕或者是遭遇攻击时）
	 * @param file
	 */
	public void delete(File file){
		SQLiteDatabase db = dbOpenHelper.getWritableDatabase();
		db.execSQL("delete from uploadLog where path=?", new Object[]{file.getAbsolutePath()});
	}
 }