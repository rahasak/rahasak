package com.score.chatz.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.score.chatz.R;
import com.score.chatz.pojo.Contact;

import java.util.ArrayList;

/**
 * Created by eranga on 12/10/16.
 */
class ContactUserListAdapter extends BaseAdapter implements Filterable {

    private Context context;
    private ContactFilter contactFilter;
    private ArrayList<Contact> contactList;
    private ArrayList<Contact> filteredList;

    private Typeface typeface;

    ContactUserListAdapter(Context context, ArrayList<Contact> contactList) {
        this.context = context;
        this.contactList = contactList;
        this.filteredList = contactList;

        this.typeface = Typeface.createFromAsset(context.getAssets(), "fonts/GeosansLight.ttf");
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary calls
        // to findViewById() on each row.
        final ViewHolder holder;

        final Contact contact = (Contact) getItem(position);

        if (convertView == null) {
            //inflate sensor list row layout
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.contacts_list_item, parent, false);

            //create view holder to store reference to child views
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.contact_name);
            holder.phoneNo = (TextView) convertView.findViewById(R.id.contact_number);

            convertView.setTag(holder);
        } else {
            //get view holder back_icon
            holder = (ViewHolder) convertView.getTag();
        }

        // set texts
        holder.name.setText(contact.getName());
        holder.phoneNo.setText(contact.getPhoneNo());

        // set fonts
        holder.name.setTypeface(typeface);
        holder.phoneNo.setTypeface(typeface);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (contactFilter == null) {
            contactFilter = new ContactFilter();
        }

        return contactFilter;
    }

    /**
     * Keep reference to children view to avoid unnecessary calls
     */
    static class ViewHolder {
        TextView name;
        TextView phoneNo;
    }

    /**
     * Custom filter for contact list
     * Filter content in contact list according to the search text
     */
    private class ContactFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if (constraint != null && constraint.length() > 0) {
                ArrayList<Contact> tempList = new ArrayList<>();

                // search content in friend list
                for (Contact contact : contactList) {
                    if (contact.getName().toLowerCase().contains(constraint.toString().toLowerCase())) {
                        tempList.add(contact);
                    }
                }

                filterResults.count = tempList.size();
                filterResults.values = tempList;
            } else {
                filterResults.count = contactList.size();
                filterResults.values = contactList;
            }

            return filterResults;
        }

        /**
         * Notify about filtered list to ui
         *
         * @param constraint text
         * @param results    filtered result
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredList = (ArrayList<Contact>) results.values;
            notifyDataSetChanged();
        }
    }

}
