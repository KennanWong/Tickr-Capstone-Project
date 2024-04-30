import React from "react";

import { apiFetch, checkValidEmail, getToken, getUserData, setFieldInState } from "../Helpers";
import Grid from "@mui/material/Unstable_Grid2";
import { H3 } from "../Styles/HelperStyles";
import ListItemText from "@mui/material/ListItemText";
import DeleteIcon from "@mui/icons-material/Delete";
import IconButton from "@mui/material/IconButton";
import { Box, FormLabel, List, ListItem } from "@mui/material";
import { borderRadius, styled, alpha } from '@mui/system';
import { ContrastInput, ContrastInputWrapper, DeleteButton, FormInput, TextButton, TkrButton } from '../Styles/InputStyles';
import ListItemIcon from '@mui/material/ListItemIcon';
import UserAvatar from "./UserAvatar"; 
import AdminItem from "./AdminItem";

export default function AdminsBar ({adminsList, editable=false, removeAdmin=null, newAdmins=[], editEvent=false, openHostMenu=null, setNewHost=null}) {
  return (
    <>
    {(adminsList.length !== 0)
      ? <div>
          {editable
            ? <>
              <h3>Admin List:</h3>
              <List>
                {adminsList.map((value, key) => {
                  return (
                    <div key={key}>
                      <ContrastInputWrapper >
                        <AdminItem index={key} adminID={value} removeAdmin={removeAdmin} newAdmins={newAdmins} editEvent={editEvent} openHostMenu={openHostMenu} setNewHost={setNewHost}/>
                      </ContrastInputWrapper>
                      <br/>
                    </div>
                  );
                })}
              </List>
            </>
            : <>
              {adminsList.map((value, key) => {
                return (
                  <UserAvatar key={key} userId={value} size={35}/>
                );
              })}
            </>
          } 
        </div>
      : <></>
    } 
    </>
  )
}
  