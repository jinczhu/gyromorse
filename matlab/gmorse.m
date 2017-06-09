load gmorse2.txt
x=mean(gmorse_box(50:150,1));
y=mean(gmorse_box(50:150,2));
z=mean(gmorse_box(50:150,3));
w=abs(gmorse_box(:,1)-x)+abs(gmorse_box(:,2)-y)+abs(gmorse_box(:,3)-z);

s=size(x);
d=1;
n=s(d);
y=fft(x,n,d);
     y=reshape(y,prod(s(1:d-1)),n,prod(s(d+1:end))); 
     s(d)=1+fix(n/2);
     y(:,s(d)+1:end,:)=[];
  y=reshape(y,s);
